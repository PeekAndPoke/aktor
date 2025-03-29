package io.peekandpoke.reaktor.auth.provider

import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.AuthSystem
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.LoginRequest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GoogleSsoAuthProvider(
    override val id: String,
    val googleClientId: String,
    val googleClientSecret: String,
) : AuthProvider {

    companion object {
        fun AuthProviderFactory.googleSso(
            id: String = "google-sso",
            googleClientId: String,
            googleClientSecret: String,
        ) = GoogleSsoAuthProvider(
            id = id,
            googleClientId = googleClientId,
            googleClientSecret = googleClientSecret,
        )
    }

    override suspend fun <USER> login(realm: AuthSystem.Realm<USER>, request: LoginRequest): Stored<USER>? {
        val typed = (request as? LoginRequest.OAuth)
            ?: throw IllegalArgumentException("Invalid credentials")

        // TODO: check token against google

        // TODO: load user and return it

        TODO("Not yet implemented")
    }

    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            type = AuthProviderModel.TYPE_GOOGLE,
            config = buildJsonObject {
                put("client-id", googleClientId)
            }
        )
    }
}
