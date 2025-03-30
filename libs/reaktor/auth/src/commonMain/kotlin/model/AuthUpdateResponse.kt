package io.peekandpoke.reaktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthUpdateResponse(
    val success: Boolean,
)
