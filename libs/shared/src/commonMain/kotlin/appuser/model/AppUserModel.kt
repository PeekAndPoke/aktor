package io.peekandpoke.aktor.shared.appuser.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUserModel(
    val id: String,
    val name: String,
    val email: String,
) {
    companion object {
        const val USER_TYPE = "AppUser"
    }
}
