package io.peekandpoke.aktor.llms.openai

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.llms.Llm
import io.peekandpoke.aktor.llms.LlmCommons
import io.peekandpoke.aktor.shared.model.Mutable
import io.peekandpoke.aktor.shared.model.Mutable.Companion.mutable
import kotlinx.coroutines.flow.*

class OpenAiLlm(
    private val model: String,
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

    override fun getModelName(): String {
        return model
    }

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
            // Emit an initial update so that the clients can update their view
            emit(
                Llm.Update.Start(conversation.value)
            )

            fun call(): Flow<ChatCompletionChunk> {
                // https://github.com/aallam/openai-kotlin/blob/main/guides/GettingStarted.md#chat
                val request = ChatCompletionRequest(
                    model = modelId,
                    messages = mapper { conversation.value.messages.map() },
                    tools = mapper { tools.map() },
                )

                return client.chatCompletions(request)
            }

            var round = 0
            var done = false

            while (!done && round++ < 1000) {
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
                }.onEach { chunk ->
                    conversation.modify {
                        it.addOrUpdate(chunk.messages)
                    }
                }.toList()

                val merged = chunks.lastOrNull() ?: AiConversation.new("empty")

                val toolCalls = merged.messages
                    .filterIsInstance<AiConversation.Message.Assistant>()
                    .flatMap { msg -> msg.toolCalls ?: emptyList() }

                // process tool calls
                if (toolCalls.isEmpty()) {
                    done = true
                } else {
                    LlmCommons {
                        processToolCalls(conversation, tools, toolCalls)
                    }
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
            // Emit an initial update so that the clients can update their view
            emit(
                Llm.Update.Start(conversation.value)
            )

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

                val toolCalls = (response.choices.first().message.toolCalls ?: emptyList())
                    .filterIsInstance<ToolCall.Function>()
                    .map { toolCall ->
                        AiConversation.Message.ToolCall(
                            id = toolCall.id.id,
                            name = toolCall.function.name,
                            args = AiConversation.Message.ToolCall.Args.tryParseOrEmpty(toolCall.function.argumentsOrNull)
                        )
                    }

                if (toolCalls.isNotEmpty()) {
                    LlmCommons {
                        processToolCalls(conversation, tools, toolCalls)
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
}
