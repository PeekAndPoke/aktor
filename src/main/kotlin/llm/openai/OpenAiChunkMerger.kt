package io.peekandpoke.aktor.llm.openai

import com.aallam.openai.api.chat.*
import io.peekandpoke.aktor.model.AiConversation

class OpenAiChunkMerger {

    data class MergedChatCompletion(
        val completions: List<ChatCompletion>,
        val messages: List<AiConversation.Message.Assistant>,
        val toolCalls: List<ToolCall>,
    )

    operator fun <T> invoke(block: OpenAiChunkMerger.() -> T) = block()

    fun List<ChatCompletionChunk>.merge(): MergedChatCompletion {
        val completions = this.mergeInternal()

        val messages = completions.mapNotNull { c ->
            c.choices.first().message.content?.let { content ->
                AiConversation.Message.Assistant(content = content)
            }
        }

        val toolCalls = completions.flatMap {
            it.choices.first().message.toolCalls ?: emptyList()
        }

        return MergedChatCompletion(
            completions = completions,
            messages = messages,
            toolCalls = toolCalls,
        )
    }

    private fun List<ChatCompletionChunk>.mergeInternal(): List<ChatCompletion> {

        val chunks = this.groupBy { it.id }

        // finalize all chunks
        val completions = chunks.map { (id, entries) ->
            val collectedToolCalls = mutableListOf<ToolCall>()

            val modelId = entries.first().model
            val created = entries.first().created.toLong()

            var currentContent: String? = null
            var currentToolCall: ToolCall.Function? = null

            entries.forEach { entry ->
                val choice = entry.choices.first()

                choice.delta?.content?.let { content ->
                    currentContent = (currentContent ?: "") + content
                }

                when (val toolCalls = choice.delta?.toolCalls) {
                    // Was the current tool call collection finished?
                    null -> currentToolCall?.let {
                        collectedToolCalls.add(it)
                        currentToolCall = null
                    }

                    else -> {
                        toolCalls.forEach { chunk ->
                            val toolId = chunk.id
                            val toolFn = chunk.function

                            // Does a new tool call start here
                            if (toolId != null && toolFn != null) {
                                // Keep the already collected call
                                currentToolCall?.let {
                                    collectedToolCalls.add(it)
                                }

                                // Start a new collection
                                currentToolCall = ToolCall.Function(id = toolId, function = toolFn)
                            } else {

                                currentToolCall = currentToolCall?.let {
                                    it.copy(
                                        function = it.function.copy(
                                            nameOrNull = listOfNotNull(
                                                it.function.nameOrNull,
                                                chunk.function?.nameOrNull
                                            ).joinToString(""),
                                            argumentsOrNull = listOfNotNull(
                                                it.function.argumentsOrNull,
                                                chunk.function?.argumentsOrNull
                                            ).joinToString("")
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ChatCompletion(
                id = id,
                created = created,
                model = modelId,
                choices = listOf(
                    ChatChoice(
                        index = 0,
                        message = ChatMessage(
                            role = ChatRole.Assistant,
                            content = currentContent,
                            name = null,
                            functionCall = null,
                            toolCalls = collectedToolCalls,
                            toolCallId = null,
                        )
                    )
                )
            )
        }

        return completions
    }
}
