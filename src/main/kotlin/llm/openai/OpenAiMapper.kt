package io.peekandpoke.aktor.llm.openai

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Parameters
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.AiConversation
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class OpenAiMapper {
    operator fun <T> invoke(block: OpenAiMapper.() -> T) = block()

    @JvmName("mapTools")
    fun List<Llm.Tool>.map(): List<Tool>? = map { tool ->
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

    @JvmName("mapMessages")
    fun List<AiConversation.Message>.map(): List<ChatMessage> = map { msg ->
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

    fun AiConversation.Message.ToolCall.map(): ToolCall {
        return ToolCall.Function(
            id = ToolId(id),
            function = FunctionCall(
                nameOrNull = name,
                argumentsOrNull = args.print(),
            ),
        )
    }
}
