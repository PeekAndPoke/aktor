package io.peekandpoke.aktor.llm

import io.peekandpoke.aktor.model.Mutable
import io.peekandpoke.aktor.model.Mutable.Companion.mutable
import io.peekandpoke.aktor.shared.model.AiConversation
import kotlinx.coroutines.flow.Flow

class ChatBot(
    val llm: Llm,
    val conversation: Mutable<AiConversation> = AiConversation.Companion.new.mutable(),
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
                conversation = AiConversation.Companion.new.addOrUpdate(prompt).mutable()
            )
        }
    }

    fun clearConversation() {
        conversation.modify { AiConversation.Companion.new }
    }

    fun chat(prompt: String): Flow<Llm.Update> {
        conversation.modify {
            it.addOrUpdate(AiConversation.Message.User(content = prompt))
        }

        return llm.chat(conversation, streaming = streaming)
    }
}
