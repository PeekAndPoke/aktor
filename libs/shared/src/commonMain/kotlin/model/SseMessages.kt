package io.peekandpoke.aktor.shared.model

import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SseMessages {

    @Serializable
    @SerialName("ai-conversation-update")
    data class AiConversationUpdate(val data: AiConversationModel) : SseMessages()
}
