package io.peekandpoke.aktor.shared.credentials.model

import kotlinx.serialization.Serializable

interface GoogleOAuthFlow {
    @Serializable
    data class InitRequest(
        val scopes: Set<String>,
        val redirectUri: String,
    )

    @Serializable
    data class InitResponse(
        val state: String,
        val url: String,
    )

    @Serializable
    data class CompleteRequest(
        val state: String,
        val code: String,
    )

    @Serializable
    data class CompleteResponse(
        val success: Boolean,
    )
}

