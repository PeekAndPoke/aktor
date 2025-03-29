package io.peekandpoke.reaktor.auth.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthProviderModel(
    val id: String,
    val type: String,
    val config: JsonObject? = null,
) {
    companion object {
        const val TYPE_EMAIL_PASSWORD = "email-password"
        const val TYPE_GOOGLE = "google"
    }
}
