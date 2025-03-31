package io.peekandpoke.aktor.llms

import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.shared.model.Mutable
import kotlinx.coroutines.flow.FlowCollector

object LlmCommons {

    inline operator fun invoke(block: LlmCommons.() -> Unit) = block()

    suspend fun FlowCollector<Llm.Update>.processToolCalls(
        conversation: Mutable<AiConversation>,
        tools: List<Llm.Tool>,
        toolCalls: List<AiConversation.Message.ToolCall>,
    ) {
        toolCalls.forEach { toolCall ->
            processToolCall(conversation, tools, toolCall)
        }
    }

    suspend fun FlowCollector<Llm.Update>.processToolCall(
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
