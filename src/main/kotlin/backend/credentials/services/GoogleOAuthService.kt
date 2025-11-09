package io.peekandpoke.aktor.backend.credentials.services

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Userinfo
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.remote.buildUri
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.aktor.backend.credentials.db.UserCredentials
import io.peekandpoke.aktor.backend.credentials.db.UserCredentialsRepo
import io.peekandpoke.aktor.shared.credentials.model.UserCredentialsModel
import kotlin.time.Duration.Companion.seconds


class GoogleOAuthService(
    val clientId: String,
    val clientSecret: String,
    val appName: String,
    userCredentialsRepo: Lazy<UserCredentialsRepo>,
) {
    private val userCredentialsRepo by userCredentialsRepo

    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    fun createAuthUrl(state: String, redirectUri: String, scopes: Set<String>): String {
        val scope = listOf(
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/userinfo.email"
        ).plus(scopes)
            .joinToString(" ")

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
        val tokenType = tokenResponse.tokenType
        val accessToken = tokenResponse.accessToken
        val refreshToken = tokenResponse.refreshToken // may be null if previously granted
        val expiresIn = tokenResponse.expiresInSeconds ?: 60L
        val expiresAt = MpInstant.now().plus(expiresIn.seconds)
        val scopes = tokenResponse.scope?.split(" ")?.toSet() ?: emptySet()

        val remoteUserInfo = getUserInfo(accessToken)
        val userInfos = UserCredentialsModel.UserInfos(
            accountId = remoteUserInfo?.id,
            email = remoteUserInfo?.email,
            name = remoteUserInfo?.name,
            pictureUrl = remoteUserInfo?.picture,
        )

        // Does it already exist for the user and the external account id?
        val existing = userCredentialsRepo.findByUserId(userId)
            .mapNotNull { it.castTyped<UserCredentials.GoogleOAuth2>() }
            .firstOrNull {
                it.value.userInfos.accountId == userInfos.accountId || it.value.userInfos.email == userInfos.email
            }

        val userCredential = when (existing) {
            null -> {
                // Insert credentials into the database
                userCredentialsRepo.insert(
                    UserCredentials.GoogleOAuth2(
                        userId = userId,
                        userInfos = userInfos,
                        tokenType = tokenType,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresAt = expiresAt,
                        scopes = scopes,
                    )
                )
            }

            else -> {
                // Update credentials into the database
                userCredentialsRepo.save(existing) {
                    it.copy(
                        userInfos = userInfos,
                        tokenType = tokenType,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresAt = expiresAt,
                        scopes = scopes,
                    )
                }
            }
        }

        return userCredential
    }

    fun getUserInfo(accessToken: String): Userinfo? {
        val tokenServerUrl = GenericUrl("https://oauth2.googleapis.com/token")
        val clientAuth = ClientParametersAuthentication(clientId, clientSecret)

        val credential = Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(httpTransport)
            .setJsonFactory(jsonFactory)
            .setTokenServerUrl(tokenServerUrl)
            .setClientAuthentication(clientAuth)
            .build()
            .setAccessToken(accessToken)

        val oauth2 = Oauth2.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(appName)
            .build()

        val response = oauth2.userinfo().get().execute()

        return response
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
