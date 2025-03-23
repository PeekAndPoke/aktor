package io.peekandpoke.aktor.shared.llms.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisteredLlmModel(
    val id: String,
    val description: String,
)
