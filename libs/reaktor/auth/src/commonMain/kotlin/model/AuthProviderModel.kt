package io.peekandpoke.reaktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthProviderModel(
    val type: String,
    val config: Map<String, String>,
) {
    companion object {
        const val TYPE_EMAIL_PASSWORD = "email-password"
    }
}
