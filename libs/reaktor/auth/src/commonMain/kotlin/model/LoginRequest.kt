package io.peekandpoke.reaktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LoginRequest {

    @Serializable
    @SerialName("email_and_password")
    data class EmailAndPassword(
        val email: String,
        val password: String,
    ) : LoginRequest
}
