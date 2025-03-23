package io.peekandpoke.aktor.llm

import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import kotlinx.coroutines.flow.Flow

class ChatBot(
    val llm: Llm,
    val streaming: Boolean,
) {
    companion object {
        val defaultSystemPrompt = """
            You are a helpful assistant.
            
            Before answering questions about the present or the future: 
            - Always get the current date and time first,
            - Always get the current location of the user first.
        """.trimIndent()

        val defaultSystemMessage = AiConversation.Message.System(content = defaultSystemPrompt)
    }

    fun chat(conversation: AiConversation, prompt: String, tools: List<Llm.Tool>): Flow<Llm.Update> {
        return llm.chat(
            conversation = conversation.addOrUpdate(
                AiConversation.Message.User(content = prompt)
            ),
            tools = tools,
            streaming = streaming,
        )
    }
}
