package io.peekandpoke.aktor.shared.aiconversation.model

import kotlinx.serialization.Serializable

object AiConversationRequest {
    @Serializable
    data class Send(
        val llmId: String?,
        val message: String,
    )

    @Serializable
    data object Create

    @Serializable
    data class Response(
        val conversation: AiConversationModel,
    )
}
