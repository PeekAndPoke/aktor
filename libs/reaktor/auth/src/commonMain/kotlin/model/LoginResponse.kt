package io.peekandpoke.reaktor.auth.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
data class LoginResponse(
    val token: String,
    val user: JsonObject,
) {
    fun <T> getTypedUser(serializer: DeserializationStrategy<T>): T {
        return Json.decodeFromJsonElement(serializer, user)
    }
}
