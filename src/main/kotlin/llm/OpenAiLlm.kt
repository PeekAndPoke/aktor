package io.peekandpoke.aktor.llm

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Parameters
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class OpenAiLlm(
    override val model: String,
    override val tools: List<Llm.Tool>,
    private val authToken: String,
    private val client: OpenAI = createDefaultClient(authToken),
) : Llm, AutoCloseable {
    companion object {
        private val jsonMapper = ObjectMapper()

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

        val mappedTools = tools.map { tool ->
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

        fun AiConversation.map() = messages.map { msg ->
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
                    toolCalls = msg.toolCalls?.map { call ->
                        ToolCall.Function(
                            id = ToolId(call.id),
                            function = FunctionCall(
                                nameOrNull = call.name,
                                argumentsOrNull = call.arguments,
                            ),
                        )
                    }
                )

                is AiConversation.Message.Tool -> {
                    val toolId = msg.toolCallId?.let { ToolId(it) }!!

                    ChatMessage(
                        role = ChatRole.Tool,
                        name = msg.name,
                        content = msg.content,
                        toolCallId = toolId,
                    )
                }
            }
        }

        return flow {

            suspend fun call(): ChatCompletion {
                // https://github.com/aallam/openai-kotlin/blob/main/guides/GettingStarted.md#chat
                val request = ChatCompletionRequest(
                    model = modelId,
                    messages = conversation.value.map(),
                    tools = mappedTools,
                )

                return client.chatCompletion(request)
            }

            suspend fun callStreaming(): List<ChatCompletionChunk> {
                // https://github.com/aallam/openai-kotlin/blob/main/guides/GettingStarted.md#chat
                val request = ChatCompletionRequest(
                    model = modelId,
                    messages = conversation.value.map(),
                    tools = mappedTools,
                )

//                println("========================================================")
//                request.messages.forEach { msg -> println(msg) }

                return client.chatCompletions(request)
                    .onEach { chunk ->
//                        println("Chunk: $chunk")

                        val delta = chunk.choices.firstOrNull()?.delta
                        val content = delta?.content

                        if (content != null) {
                            emit(
                                Llm.Update.Response(content)
                            )
                        }
                    }
                    .fold(emptyList()) { acc, msg ->
                        acc + msg
                    }
            }

            var done = false

            while (!done) {
                val response = call()

                val toolCalls = response.choices.first().message.toolCalls ?: emptyList()

                if (toolCalls.isNotEmpty()) {
                    toolCalls.forEach { toolCall: ToolCall ->
//                        println(toolCall)

                        when (toolCall) {
                            is ToolCall.Function -> {
                                val fn = toolCall.function

                                val fnName = fn.name
                                val callId = toolCall.id

                                val fnArgsRaw = fn.argumentsOrNull
                                val fnArgs: Map<String, Any?> = try {
                                    jsonMapper.readValue(content = fnArgsRaw ?: "")
                                } catch (t: Throwable) {
                                    emptyMap()
                                }

                                emit(
                                    Llm.Update.Info(
                                        "Tool call: $fnName | Id: ${callId.id} | Args: $fnArgs"
                                    )
                                )

                                val tool = tools.firstOrNull { it.name == fnName }

                                val params = Llm.Tool.CallParams(fnArgs)

                                val toolResult = when (tool) {
                                    null -> "Error! The tool $fnName is not available."
                                    else -> try {
                                        tool.call(params)
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
                                                    id = callId.id,
                                                    name = fnName,
                                                    arguments = fnArgsRaw,
                                                )
                                            )
                                        )
                                    )
                                }

                                conversation.modify {
                                    it.add(
                                        AiConversation.Message.Tool(
                                            toolCallId = callId.id,
                                            name = fnName,
                                            content = toolResult,
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val answer = response.choices.first().message.content?.takeIf { it.isNotBlank() }

                    emit(
                        Llm.Update.Response(answer.orEmpty())
                    )

                    conversation.modify {
                        it.add(AiConversation.Message.Assistant(content = answer))
                    }

                    done = true
                }
            }

            emit(Llm.Update.Stop)
        }
    }
}
