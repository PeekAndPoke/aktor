package io.peekandpoke.aktor.llm

import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import io.peekandpoke.aktor.model.OllamaModels
import kotlinx.coroutines.flow.Flow

interface Llm {

    class Tool(
        val tool: OllamaModels.Tool,
        val fn: suspend (Params) -> String,
    ) {
        class Params(
            val params: Map<String, String>,
        )
    }

    sealed interface Update {
        data class Response(
            val response: OllamaModels.ChatResponse
        ): Update

        data class Info(
            val message: String,
        ): Update

        data object Stop: Update
    }

    fun chat(
        conversation: Mutable<AiConversation>,
    ): Flow<Update>

    val model: String
    val tools: List<Tool>
}

