package io.peekandpoke.aktor.shared.model

import kotlinx.serialization.Serializable


object AiConversationRequest {

    @Serializable
    data class Send(
        val message: String,
    )

    @Serializable
    data object Create

    @Serializable
    data class Response(
        val conversation: AiConversationModel,
    )
}
