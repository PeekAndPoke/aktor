package io.peekandpoke.aktor.model

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class AiResponse(
    val model: String,
    val created_at: String,
    val message: Message,
    val done: Boolean,
    val done_reason: String? = null,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Long? = null,
    val prompt_eval_duration: Long? = null,
    val eval_count: Long? = null,
    val eval_duration: Long? = null,
) {
    @Serializable
    data class Message(
        val role: String,
        val content: String? = null,
        val tool_calls: List<ToolCall>? = null,
    )

    @Serializable
    data class ToolCall(
        val function: Function,
    ) {
        @Serializable
        data class Function(
            val name: String,
            val arguments: Map<String, String>? = null,
        )
    }
}
