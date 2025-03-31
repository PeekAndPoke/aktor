package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthUpdateResponse(
    val success: Boolean,
)
