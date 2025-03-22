package io.peekandpoke.aktor.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SseMessages {

    @Serializable
    @SerialName("ai-conversation-update")
    data class AiConversationUpdate(val data: AiConversationModel) : SseMessages()
}
