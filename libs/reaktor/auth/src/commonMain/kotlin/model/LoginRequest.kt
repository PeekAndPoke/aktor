package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LoginRequest {

    @Serializable
    @SerialName("email_and_password")
    data class EmailAndPassword(
        override val provider: String,
        val email: String,
        val password: String,
    ) : LoginRequest

    @Serializable
    @SerialName("oauth")
    data class OAuth(
        override val provider: String,
        val token: String,
    ) : LoginRequest

    val provider: String
}
