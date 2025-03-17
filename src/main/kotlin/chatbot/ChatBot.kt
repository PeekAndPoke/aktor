package io.peekandpoke.aktor.chatbot

import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import io.peekandpoke.aktor.model.Mutable.Companion.mutable
import kotlinx.coroutines.flow.Flow

class ChatBot(
    val llm: Llm,
    val conversation: Mutable<AiConversation> = AiConversation.new.mutable(),
    val streaming: Boolean,
) {
    companion object {
        fun of(
            llm: Llm,
            streaming: Boolean,
            systemPrompt: AiConversation.Message.System? = null,
        ): ChatBot {
            val prompt = systemPrompt ?: AiConversation.Message.System(
                content = """
                    You are a helpful assistant.
                    
                    Before answering questions about the present or the future: 
                    - Always get the current date and time first,
                    - Always get the current location of the user first.
                """.trimIndent()
            )
            return ChatBot(
                llm = llm,
                streaming = streaming,
                conversation = AiConversation.new.addOrUpdate(prompt).mutable()
            )
        }
    }

    fun clearConversation() {
        conversation.modify { AiConversation.new }
    }

    fun chat(prompt: String): Flow<Llm.Update> {
        conversation.modify {
            it.addOrUpdate(AiConversation.Message.User(content = prompt))
        }

        return llm.chat(conversation, streaming = streaming)
    }
}
