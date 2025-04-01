package io.peekandpoke.aktor.llms.openai

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Parameters
import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.llms.Llm

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
                    parameters = Parameters(
                        tool.createJsonSchema()
                    ),
                ),
            )
        }
    }.takeIf { it.isNotEmpty() }

    @JvmName("mapMessages")
    fun List<AiConversation.Message>.map(): List<ChatMessage> = mapNotNull { msg ->
        when (msg) {
            is AiConversation.Message.System ->
                ChatMessage.System(content = msg.content)

            is AiConversation.Message.User ->
                ChatMessage.User(content = msg.content)

            is AiConversation.Message.Assistant ->
                ChatMessage.Assistant(content = msg.content, toolCalls = msg.toolCalls?.map())

            is AiConversation.Message.Tool ->
                ChatMessage.Tool(content = msg.content, toolCallId = ToolId(msg.toolCall.id))
        }
    }

    private fun List<AiConversation.Message.ToolCall>.map(): List<ToolCall>? =
        map { call -> call.map() }.takeIf { it.isNotEmpty() }

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
