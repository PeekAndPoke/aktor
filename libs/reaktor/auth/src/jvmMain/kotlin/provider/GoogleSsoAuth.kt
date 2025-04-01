package de.peekandpoke.funktor.auth.provider

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import com.google.api.client.json.gson.GsonFactory
import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.model.AuthLoginRequest
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.GeneralSecurityException
import java.util.*

class GoogleSsoAuth(
    override val id: String,
    val googleClientId: String,
) : AuthProvider {

    class Factory {
        operator fun invoke(
            id: String = "google-sso",
            googleClientId: String,
        ) = GoogleSsoAuth(
            id = id,
            googleClientId = googleClientId,
        )
    }

    override suspend fun <USER> login(realm: AuthRealm<USER>, request: AuthLoginRequest): Stored<USER>? {
        val typed = (request as? AuthLoginRequest.OAuth)
            ?: throw AuthError.invalidCredentials()

        val verifier = GoogleIdTokenVerifier
            .Builder(ApacheHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build()

        val idToken: GoogleIdToken = try {
            verifier.verify(typed.token)
        } catch (e: GeneralSecurityException) {
            throw AuthError.invalidCredentials(e)
        }

        val payload = idToken.payload

        return realm.loadUserByEmail(payload.email)
            ?: throw AuthError.invalidCredentials()
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
