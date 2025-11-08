package io.peekandpoke.aktor.shared.credentials.model

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserCredentialsModel {

    @Serializable
    @SerialName("google_oauth2")
    data class GoogleOAuth2(
        override val id: String,
        override val userId: String,
        val expiresAt: MpInstant,
        val scopes: List<String>,
    ) : UserCredentialsModel

    val id: String
    val userId: String
}
