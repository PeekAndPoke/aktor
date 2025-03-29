package io.peekandpoke.reaktor.auth.provider

import de.peekandpoke.ultra.vault.Stored
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.peekandpoke.reaktor.auth.AuthError
import io.peekandpoke.reaktor.auth.AuthSystem
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.LoginRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class GithubSsoAuth(
    override val id: String,
    val githubClientId: String,
    val githubClientSecret: String,
) : AuthProvider {

    class Factory {
        operator fun invoke(
            id: String = "github-sso",
            githubClientId: String,
            githubClientSecret: String,
        ) = GithubSsoAuth(
            id = id,
            githubClientId = githubClientId,
            githubClientSecret = githubClientSecret,
        )
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun <USER> login(realm: AuthSystem.Realm<USER>, request: LoginRequest): Stored<USER>? {
        val typed = (request as? LoginRequest.OAuth)
            ?: throw AuthError.invalidCredentials()

        // see https://medium.com/@r.sadatshokouhi/implementing-sso-in-react-with-github-oauth2-4d8dbf02e607

        val ghAccessToken = getAccessToken(typed.token) ?: throw AuthError.invalidCredentials()

        val ghUser = getUser(ghAccessToken) ?: throw AuthError.invalidCredentials()

        val email = ghUser["email"]?.jsonPrimitive?.content ?: throw AuthError.invalidCredentials()

        return realm.loadUserByEmail(email)
            ?: throw AuthError.invalidCredentials()
    }

    private suspend fun getAccessToken(code: String): JsonObject? {
        val response = httpClient.post("https://github.com/login/oauth/access_token") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("client_id", githubClientId)
                put("client_secret", githubClientSecret)
                put("code", code)
            })
        }

        if (!response.status.isSuccess()) return null

        return response.body<JsonObject?>()
    }

    private suspend fun getUser(access: JsonObject): JsonObject? {
        val tokenType = access["token_type"]?.jsonPrimitive?.content
        val accessToken = access["access_token"]?.jsonPrimitive?.content

        val response = httpClient.get("https://api.github.com/user") {
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "$tokenType $accessToken")
        }

        if (!response.status.isSuccess()) return null

        return response.body<JsonObject?>()
    }

    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            type = AuthProviderModel.TYPE_GITHUB,
            config = buildJsonObject {
                put("client-id", githubClientId)
            }
        )
    }
}
