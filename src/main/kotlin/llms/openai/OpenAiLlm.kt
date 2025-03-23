package io.peekandpoke.aktor.llm.openai

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.shared.model.Mutable
import io.peekandpoke.aktor.shared.model.Mutable.Companion.mutable
import kotlinx.coroutines.flow.*

class OpenAiLlm(
    override val model: String,
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

    override fun close() {
        client.close()
    }

    override fun chat(
        conversation: AiConversation,
        tools: List<Llm.Tool>,
        streaming: Boolean,
    ): Flow<Llm.Update> {

        val toolsByName = tools.associateBy { it.name }
        val matchedTools = conversation.tools.mapNotNull { toolsByName[it.name] }

        return chatInternal(conversation.mutable(), matchedTools, streaming)
    }

    private fun chatInternal(
        conversation: Mutable<AiConversation>,
        tools: List<Llm.Tool>,
        streaming: Boolean,
    ): Flow<Llm.Update> {
        return if (streaming) {
            chatStreaming(conversation, tools)
        } else {
            chatNonStreaming(conversation, tools)
        }
    }

    private fun chatStreaming(
        conversation: Mutable<AiConversation>,
        tools: List<Llm.Tool>,
    ): Flow<Llm.Update> {
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
                val merger = OpenAiProgressiveChunkMerger()

                // Collect all chunks
                val chunks = call().onEach { chunk ->
                    val choice = chunk.choices.first()

                    choice.delta?.content?.let { content ->
                        emit(
                            Llm.Update.Response(conversation.value, content)
                        )
                    }
                }.map { chunk ->
                    merger.process(chunk = chunk)
                        .also { partial -> conversation.modify { it.addOrUpdate(partial.messages) } }
                }.toList()

                val merged = chunks.lastOrNull() ?: AiConversation.new("empty")

                val toolCalls = merged.messages
                    .filterIsInstance<AiConversation.Message.Assistant>()
                    .flatMap { msg -> msg.toolCalls ?: emptyList() }

                // process tool calls
                if (toolCalls.isNotEmpty()) {
                    toolCalls.forEach { toolCall ->
                        processToolCall(conversation, tools, toolCall)
                    }
                } else {
                    done = true
                }
            }

            emit(
                Llm.Update.Stop(conversation.value)
            )
        }
    }

    private fun chatNonStreaming(
        conversation: Mutable<AiConversation>,
        tools: List<Llm.Tool>,
    ): Flow<Llm.Update> {
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
                    toolCalls
                        .filterIsInstance<ToolCall.Function>()
                        .map { toolCall ->
                            AiConversation.Message.ToolCall(
                                id = toolCall.id.id,
                                name = toolCall.function.name,
                                args = AiConversation.Message.ToolCall.Args.tryParseOrEmpty(toolCall.function.argumentsOrNull)
                            )
                        }
                        .forEach { toolCall ->
                            processToolCall(conversation, tools, toolCall)
                        }
                } else {
                    val answer = response.choices.first().message.content

                    emit(
                        Llm.Update.Response(conversation.value, answer.orEmpty())
                    )

                    conversation.modify {
                        it.addOrUpdate(AiConversation.Message.Assistant(content = answer))
                    }

                    done = true
                }
            }

            emit(
                Llm.Update.Stop(conversation.value)
            )
        }
    }

    private suspend fun FlowCollector<Llm.Update>.processToolCall(
        conversation: Mutable<AiConversation>,
        tools: List<Llm.Tool>,
        toolCall: AiConversation.Message.ToolCall,
    ) {
        val fnName = toolCall.name
        val fnCallId = toolCall.id
        val fnArgs = toolCall.args

        emit(
            Llm.Update.Info(
                conversation = conversation.value,
                message = "Tool call: $fnName | Id: $fnCallId | Args: ${fnArgs.print()}",
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

        conversation.modify {
            it.addOrUpdate(
                AiConversation.Message.Tool(
                    content = toolResult,
                    toolCall = AiConversation.Message.ToolCall(id = fnCallId, name = fnName, args = fnArgs),
                )
            )
        }

        emit(
            Llm.Update.Info(
                conversation = conversation.value,
                message = "Tool result: $toolResult",
            )
        )
    }
}
