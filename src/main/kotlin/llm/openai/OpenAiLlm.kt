package io.peekandpoke.aktor.llm.openai

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Parameters
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

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

    override fun close() {
        client.close()
    }

    override fun chat(conversation: Mutable<AiConversation>): Flow<Llm.Update> {

        val modelId = ModelId(model)

        return flow {
            suspend fun call(): ChatCompletion {
                // https://github.com/aallam/openai-kotlin/blob/main/guides/GettingStarted.md#chat
                val request = ChatCompletionRequest(
                    model = modelId,
                    messages = conversation.value.mapMessages(),
                    tools = tools.map(),
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

    private fun List<Llm.Tool>.map(): List<Tool>? = map { tool ->
        when (tool) {
            is Llm.Tool.Function -> Tool(
                type = ToolType.Function,
                function = FunctionTool(
                    name = tool.name,
                    description = tool.description,
                    parameters = Parameters.buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            tool.parameters.forEach { p ->
                                when (p) {
                                    is Llm.Tool.StringParam -> putJsonObject(p.name) {
                                        put("type", "string")
                                        put("description", p.description)
                                    }

                                    is Llm.Tool.IntegerParam -> putJsonObject(p.name) {
                                        put("type", "integer")
                                        put("description", p.description)
                                    }

                                    is Llm.Tool.BooleanParam -> putJsonObject(p.name) {
                                        put("type", "boolean")
                                        put("description", p.description)
                                    }
                                }
                            }
                        }
                        putJsonArray("required") {
                            tool.parameters.filter { it.required }.forEach { p ->
                                add(p.name)
                            }
                        }
                    }
                ),
            )
        }
    }.takeIf { it.isNotEmpty() }

    private fun AiConversation.mapMessages() = messages.map { msg ->
        when (msg) {
            is AiConversation.Message.System -> ChatMessage(
                role = ChatRole.System,
                content = msg.content,
            )

            is AiConversation.Message.User -> ChatMessage(
                role = ChatRole.User,
                content = msg.content,
            )

            is AiConversation.Message.Assistant -> ChatMessage(
                role = ChatRole.Assistant,
                content = msg.content,
                toolCalls = msg.toolCalls?.map { call -> call.map() }?.takeIf { it.isNotEmpty() },
            )

            is AiConversation.Message.Tool -> {
                ChatMessage(
                    role = ChatRole.Tool,
                    name = msg.name,
                    content = msg.content,
                    toolCallId = msg.toolCall?.id?.let { ToolId(it) },
                )
            }
        }
    }

    private fun AiConversation.Message.ToolCall.map(): ToolCall {
        return ToolCall.Function(
            id = ToolId(id),
            function = FunctionCall(
                nameOrNull = name,
                argumentsOrNull = args.print(),
            ),
        )
    }
}
