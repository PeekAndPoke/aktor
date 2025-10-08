package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthSignupRequest {
    val provider: String

    @Serializable
    @SerialName("email_and_password")
    data class EmailAndPassword(
        override val provider: String,
        val email: String,
        val displayName: String? = null,
        val password: String,
    ) : AuthSignupRequest

    @Serializable
    @SerialName("oauth")
    data class OAuth(
        override val provider: String,
        val token: String,
    ) : AuthSignupRequest
}

@Serializable
data class AuthSignupResponse(
    val success: Boolean,
    val userId: String? = null,
    val requiresActivation: Boolean = false,
) {
    companion object {
        val failed = AuthSignupResponse(success = false)
    }
}

@Serializable
data class AuthActivateRequest(
    val token: String,
)

@Serializable
data class AuthActivateResponse(
    val success: Boolean,
)
