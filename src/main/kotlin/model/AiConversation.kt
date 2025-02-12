package io.peekandpoke.aktor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

data class AiConversation(
    val messages: List<Message>,
) {
    companion object {
        val new = AiConversation(
            messages = emptyList(),
        )
    }

    @Serializable
    @JsonClassDiscriminator("role")
    sealed interface Message {
        @Serializable
        @SerialName("system")
        data class System(
            val content: String,
        ) : Message

        @Serializable
        @SerialName("assistant")
        data class Assistant(
            val content: String,
        ): Message

        @Serializable
        @SerialName("user")
        data class User(
            val content: String,
        ): Message

        @Serializable
        @SerialName("tool")
        data class Tool(
            val name: String,
            val content: String,
        ): Message
    }

//    @Serializable
//    data class AiMessage(
//        val role: String,
//        val content: String,
//        val name: String? = null,
//        val tool_calls: List<ToolCall>? = null,
//    ) {
//        @Serializable
//        data class ToolCall(
//            val function: Function,
//        ) {
//            @Serializable
//            data class Function(
//                val name: String,
//                val arguments: Map<String, String>? = null,
//            )
//        }
//    }

    fun add(message: Message): AiConversation = copy(
        messages = messages + message
    )
}
