package io.peekandpoke.aktor.llms.openai

import com.aallam.openai.api.chat.ChatChunk
import com.aallam.openai.api.chat.ChatCompletionChunk
import de.peekandpoke.ultra.common.replaceFirstByOrAdd
import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.shared.model.Mutable
import io.peekandpoke.aktor.shared.model.Mutable.Companion.mutable

class OpenAiProgressiveChunkMerger {

    private val conversation: Mutable<AiConversation> = AiConversation.new("merger").mutable()

    private var messages = mutableMapOf<String, AiConversation.Message.Assistant>()

    private val toolCallArgs = mutableMapOf<String, String>()

    fun process(chunk: ChatCompletionChunk): AiConversation {

        val choice = chunk.choices.firstOrNull() ?: return conversation.value

        val completionId = chunk.id
        val message = messages.getOrPut(completionId) {
            AiConversation.Message.Assistant()
        }

        val modified = message.modify(
            { appendContent(choice.delta?.content.orEmpty()) },
            { appendToolCalls(choice) }
        )

        // Update the message
        messages[completionId] = modified
        // Modify the conversation
        conversation.modify { it.addOrUpdate(modified) }
        // Return what we have so far
        return conversation.value
    }

    private fun AiConversation.Message.Assistant.modify(
        vararg fn: AiConversation.Message.Assistant.() -> AiConversation.Message.Assistant,
    ): AiConversation.Message.Assistant {
        return fn.fold(this) { acc, f -> f(acc) }
    }

    private fun AiConversation.Message.Assistant.appendContent(choice: ChatChunk): AiConversation.Message.Assistant {
        return appendContent(choice.delta?.content.orEmpty())
    }

    private fun AiConversation.Message.Assistant.appendToolCalls(choice: ChatChunk): AiConversation.Message.Assistant {
        val deltaToolCalls = choice.delta?.toolCalls ?: return this

        return deltaToolCalls.fold(this) { accMsg, toolCallChunk ->

            val toolCallId = toolCallChunk.id?.id
            val toolCallFn = toolCallChunk.function

            val call = accMsg.toolCalls?.firstOrNull { it.id == toolCallId }
                ?: if (toolCallId != null && toolCallFn != null) {
                    AiConversation.Message.ToolCall(
                        id = toolCallId,
                        name = toolCallFn.name,
                        args = AiConversation.Message.ToolCall.Args.empty,
                    )
                } else {
                    accMsg.toolCalls?.last()
                }

            when (call) {
                null -> accMsg

                else -> {
                    val args = toolCallChunk.function?.argumentsOrNull

                    val mergedArgs = toolCallArgs[call.id].orEmpty() + args.orEmpty()

                    toolCallArgs[call.id] = mergedArgs

                    val updatedCall = call.copy(
                        args = AiConversation.Message.ToolCall.Args.tryParseOrEmpty(mergedArgs)
                    )

                    accMsg.copy(
                        toolCalls = (accMsg.toolCalls ?: emptyList())
                            .replaceFirstByOrAdd(updatedCall) { it.id == call.id }
                    )
                }
            }
        }
    }
}
