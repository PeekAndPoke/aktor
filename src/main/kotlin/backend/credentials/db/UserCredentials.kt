package io.peekandpoke.aktor.backend.credentials.db

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Vault
import de.peekandpoke.ultra.vault.hooks.Timestamped
import io.peekandpoke.aktor.shared.credentials.model.UserCredentialsModel
import kotlinx.serialization.SerialName

@Vault
sealed interface UserCredentials : Timestamped {

    @SerialName("google_oauth2")
    data class GoogleOAuth2(
        override val userId: String,
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: MpInstant,
        val scopes: List<String>,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
    ) : UserCredentials {
        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)

        override fun asApiModel(storable: Storable<UserCredentials>) = UserCredentialsModel.GoogleOAuth2(
            id = storable._id,
            userId = userId,
            expiresAt = expiresAt,
            scopes = scopes,
        )
    }

    @Vault.Field
    val userId: String

    fun asApiModel(storable: Storable<UserCredentials>): UserCredentialsModel
}

fun Storable<UserCredentials>.asApiModel() = value.asApiModel(this)
