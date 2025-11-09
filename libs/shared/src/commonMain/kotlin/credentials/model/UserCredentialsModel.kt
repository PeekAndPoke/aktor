package io.peekandpoke.aktor.shared.credentials.model

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserCredentialsModel {

    @Serializable
    data class UserInfos(
        val accountId: String?,
        val name: String?,
        val email: String?,
        val pictureUrl: String?,
    ) {
        companion object {
            val empty = UserInfos(null, null, null, null)
        }
    }

    @Serializable
    @SerialName("google_oauth2")
    data class GoogleOAuth2(
        override val id: String,
        override val userId: String,
        override val userInfos: UserInfos,
        val expiresAt: MpInstant,
        val scopes: Set<String>,
    ) : UserCredentialsModel

    val id: String
    val userId: String
    val userInfos: UserInfos
}
