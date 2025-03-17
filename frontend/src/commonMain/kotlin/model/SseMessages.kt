package io.peekandpoke.aktor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SseMessages {

    @Serializable
    @SerialName("ai-conversation")
    data class AiConversationMessage(val data: AiConversation) : SseMessages()
}
