package io.peekandpoke.aktor.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUserLoginResponse(
    val token: String,
    val user: AppUserModel,
)
