package io.peekandpoke.aktor.chatbot

import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import io.peekandpoke.aktor.model.Mutable.Companion.mutable
import io.peekandpoke.aktor.model.OllamaModels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChatBot(
    val llm: Llm,
    val conversation: Mutable<AiConversation> = AiConversation.new.mutable(),
) {
    companion object {
        fun of(
            llm: Llm,
            systemPrompt: AiConversation.Message.System? = null,
        ) = ChatBot(
            llm = llm,
            conversation = AiConversation.new
                .add(systemPrompt ?: AiConversation.Message.System("You are a helpful assistant."))
                .mutable()
        )
    }

    fun clearConversation() {
        conversation.modify { AiConversation.new }
    }

    fun chat(prompt: String): Flow<Llm.Update> {
        conversation.modify { it.add(AiConversation.Message.User(prompt)) }

        return llm.chat(conversation)
    }
}
