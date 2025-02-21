package io.peekandpoke.aktor.llm.openai

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class OpenAiLlm(
    override val model: String,
    override val tools: List<Llm.Tool>,
    private val authToken: String,
    private val client: OpenAI = createDefaultClient(authToken),
) : Llm, AutoCloseable {
    companion object {
        fun createDefaultClient(token: String): OpenAI {
            return OpenAI(
                token = token,
                logging = LoggingConfig(logLevel = LogLevel.None)
            )
        }
    }

    private val mapper = OpenAiMapper()
    private val merger = OpenAiChunkMerger()

    override fun close() {
        client.close()
    }

    override fun chat(conversation: Mutable<AiConversation>, streaming: Boolean): Flow<Llm.Update> {
        return if (streaming) {
            chatStreaming(conversation)
        } else {
            chatNonStreaming(conversation)
        }
    }

    private fun chatStreaming(conversation: Mutable<AiConversation>): Flow<Llm.Update> {
        val modelId = ModelId(model)

        return flow {
            fun call(): Flow<ChatCompletionChunk> {
                // https://github.com/aallam/openai-kotlin/blob/main/guides/GettingStarted.md#chat
                val request = ChatCompletionRequest(
                    model = modelId,
                    messages = mapper { conversation.value.messages.map() },
                    tools = mapper { tools.map() },
                )

                return client.chatCompletions(request)
            }

            var done = false

            while (!done) {
                // Collect all chunks
                val chunks = call()
                    .onEach { chunk ->
//                        println(chunk)
                        val choice = chunk.choices.first()

                        choice.delta?.content?.let { content ->
                            emit(Llm.Update.Response(content))
                        }
                    }.fold(emptyList<ChatCompletionChunk>()) { acc, chunk -> acc.plus(chunk) }

                // Combine all chunks
                val merged = merger { chunks.merge() }

                // Add all collected messages to conversation
                merged.messages.forEach { msg -> conversation.modify { it.add(msg) } }

                // process tool calls
                if (merged.toolCalls.isNotEmpty()) {
                    merged.toolCalls.forEach { toolCall -> processToolCall(conversation, toolCall) }
                } else {
                    done = true
                }
            }

            emit(Llm.Update.Stop)
        }
    }

    private fun chatNonStreaming(conversation: Mutable<AiConversation>): Flow<Llm.Update> {
        val modelId = ModelId(model)

        return flow {
            suspend fun call(): ChatCompletion {
                // https://github.com/aallam/openai-kotlin/blob/main/guides/GettingStarted.md#chat
                val request = ChatCompletionRequest(
                    model = modelId,
                    messages = mapper { conversation.value.messages.map() },
                    tools = mapper { tools.map() },
                )

                return client.chatCompletion(request)
            }

            var done = false

            while (!done) {
                val response = call()

                val toolCalls = response.choices.first().message.toolCalls ?: emptyList()

                if (toolCalls.isNotEmpty()) {
                    toolCalls.forEach { toolCall: ToolCall ->
                        processToolCall(conversation, toolCall)
                    }
                } else {
                    val answer = response.choices.first().message.content

                    emit(Llm.Update.Response(answer.orEmpty()))

                    conversation.modify {
                        it.add(AiConversation.Message.Assistant(content = answer))
                    }

                    done = true
                }
            }

            emit(Llm.Update.Stop)
        }
    }

    private suspend fun FlowCollector<Llm.Update>.processToolCall(
        conversation: Mutable<AiConversation>,
        toolCall: ToolCall,
    ) {
        when (toolCall) {
            is ToolCall.Function -> {
                val fn = toolCall.function
                val fnName = fn.name
                val fnCallId = toolCall.id
                val fnArgsRaw = fn.argumentsOrNull ?: "{}"
                val fnArgs = try {
                    Json.parseToJsonElement(fnArgsRaw).jsonObject
                } catch (t: SerializationException) {
                    JsonObject(emptyMap())
                }.let {
                    AiConversation.Message.ToolCall.Args(it)
                }

                emit(
                    Llm.Update.Info(
                        "Tool call: $fnName | Id: ${fnCallId.id} | Args: ${fnArgs.print()} | ArgsRaw: $fnArgsRaw"
                    )
                )

                val tool = tools.firstOrNull { it.name == fnName }

                val toolResult = when (tool) {
                    null -> "Error! The tool $fnName is not available."
                    else -> try {
                        tool.call(fnArgs)
                    } catch (t: Throwable) {
                        "Error! Calling the tool $fnName failed: ${t.message}."
                    }
                }

                emit(
                    Llm.Update.Info(
                        "Tool result: $toolResult"
                    )
                )

                // Add the assistant tool call to the conversation
                conversation.modify {
                    it.add(
                        AiConversation.Message.Assistant(
                            content = null,
                            toolCalls = listOf(
                                AiConversation.Message.ToolCall(
                                    id = fnCallId.id,
                                    name = fnName,
                                    args = fnArgs,
                                )
                            )
                        )
                    )
                }

                conversation.modify {
                    it.add(
                        AiConversation.Message.Tool(
                            name = fnName,
                            content = toolResult,
                            toolCall = AiConversation.Message.ToolCall(id = fnCallId.id, name = fnName, args = fnArgs),
                        )
                    )
                }
            }
        }
    }

}
