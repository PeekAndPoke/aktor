package io.peekandpoke.aktor.chatbot

import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import io.peekandpoke.aktor.model.Mutable.Companion.mutable
import io.peekandpoke.aktor.model.OllamaModels

class ChatBot(
    val model: String,
    val tools: List<Tool>,
    val conversation: Mutable<AiConversation> = AiConversation.new.mutable(),
) {
    companion object {
        fun of(
            model: String,
            tools: List<Tool> = emptyList(),
            systemPrompt: AiConversation.Message.System? = null,
        ) = ChatBot(
            model = model,
            tools = tools,
            conversation = AiConversation.new
                .add(systemPrompt ?: AiConversation.Message.System("You are a helpful assistant."))
                .mutable()

        )
    }

    class Tool(
        val tool: OllamaModels.Tool,
        val fn: (ToolParams) -> String,
    )

    class ToolParams(
        val params: Map<String, String>,
    )

    fun clearConversation() {
        conversation.modify { AiConversation.new }
    }
}
