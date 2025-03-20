package io.peekandpoke.aktor.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginWithPassword(
    val user: String = "",
    val password: String = "",
)
