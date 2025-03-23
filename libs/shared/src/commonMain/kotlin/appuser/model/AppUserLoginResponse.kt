package io.peekandpoke.aktor.shared.appuser.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUserLoginResponse(
    val token: String,
    val user: AppUserModel,
)
