package io.peekandpoke.aktor.backend.credentials.services

import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.remote.buildUri
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.aktor.backend.credentials.db.UserCredentials
import io.peekandpoke.aktor.backend.credentials.db.UserCredentialsRepo
import kotlin.time.Duration.Companion.seconds

class GoogleOAuthService(
    val clientId: String,
    val clientSecret: String,
    userCredentialsRepo: Lazy<UserCredentialsRepo>,
) {
    private val userCredentialsRepo by userCredentialsRepo

    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    fun createAuthUrl(state: String, redirectUri: String, scopes: List<String>): String {
        val scope = scopes.joinToString(" ")

        val authUrl = buildUri("https://accounts.google.com/o/oauth2/v2/auth") {
            set("client_id", clientId)
            set("redirect_uri", redirectUri)
            set("response_type", "code")
            set("scope", scope)
            set("access_type", "offline") // request refresh token
            set("prompt", "consent") // consider when you need refresh token issuance
            set("state", state)
        }

        return authUrl
    }

    suspend fun completeAuth(
        userId: String,
        code: String,
        redirectUri: String,
    ): Stored<UserCredentials.GoogleOAuth2> {
        // Exchange the code for tokens
        val tokenResponse: TokenResponse = GoogleAuthorizationCodeTokenRequest(
            httpTransport,
            jsonFactory,
            clientId,
            clientSecret,
            code,
            redirectUri,
        ).execute()

        // tokenResponse contains: accessToken, refreshToken (may be null), expiresInSeconds, scope
        val accessToken = tokenResponse.accessToken
        val refreshToken = tokenResponse.refreshToken // may be null if previously granted
        val expiresIn = tokenResponse.expiresInSeconds ?: 60L
        val expiresAt = MpInstant.now().plus(expiresIn.seconds)

        val userCredential = UserCredentials.GoogleOAuth2(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            scopes = tokenResponse.scope?.split(" ") ?: emptyList()
        )

        // Save credentials into the database
        return userCredentialsRepo.insert(userCredential)
    }

    suspend fun getCredentialsWithAllScopes(
        userId: String,
        scopes: List<String>,
    ): Stored<UserCredentials.GoogleOAuth2>? {
        val all = userCredentialsRepo.findByUserId(userId)
            .mapNotNull { it.castTyped<UserCredentials.GoogleOAuth2>() }

        val first = all.firstOrNull { it.value.scopes.containsAll(scopes) }
            ?: return null

        // check if the accessToken is still valid or refresh it
        val isExpired = MpInstant.now() > first.value.expiresAt

        val credential = if (!isExpired) {
            first
        } else {
            val refreshResponse = GoogleRefreshTokenRequest(
                httpTransport,
                jsonFactory,
                first.value.refreshToken,
                clientId,
                clientSecret
            ).execute()

            userCredentialsRepo.save(first) {
                it.copy(
                    accessToken = refreshResponse.accessToken,
                    expiresAt = MpInstant.now().plus(refreshResponse.expiresInSeconds.seconds),
                    refreshToken = refreshResponse.refreshToken ?: first.value.refreshToken,
                )
            }
        }

        return credential
    }
}
