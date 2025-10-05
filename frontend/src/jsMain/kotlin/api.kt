package de.peekandpoke.aktor.frontend

import de.peekandpoke.funktor.auth.api.AuthApiClient
import de.peekandpoke.ultra.common.remote.ApiClient.Config
import de.peekandpoke.ultra.common.remote.RemoteRequest
import de.peekandpoke.ultra.common.remote.RemoteResponse
import de.peekandpoke.ultra.common.remote.RequestInterceptor
import de.peekandpoke.ultra.common.remote.ResponseInterceptor
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import io.peekandpoke.aktor.shared.appuser.api.AppUserApiClients
import io.peekandpoke.aktor.shared.llms.api.LlmApiClients
import kotlinx.serialization.json.Json

class WebAppApis(appConfig: WebAppConfig, tokenProvider: () -> String?) {

    val codec = Json {
        classDiscriminator = "_type"
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    val config = Config(
        baseUrl = appConfig.apiBaseUrl,
        codec = codec,
        requestInterceptors = listOf(
            JwtRequestInterceptor(tokenProvider)
        ),
        responseInterceptors = listOf(
//            ApiResponseInterceptor(),
            ErrorLoggingResponseInterceptor()
        ),
        client = HttpClient {
            install(SSE) {
                showCommentEvents()
                showRetryEvents()
            }

            install(ContentNegotiation) {
                json(json = codec)
            }
        },
    )

    val auth = AuthApiClient("app-user", config)

    val appUser = AppUserApiClients(config)
    val llms = LlmApiClients(config)
}

class JwtRequestInterceptor(private val token: () -> String?) : RequestInterceptor {

    override fun intercept(request: RemoteRequest) {
        token()?.let {
            request.header("Authorization", "Bearer $it")
        }
    }
}

class ErrorLoggingResponseInterceptor : ResponseInterceptor {

    override suspend fun intercept(response: RemoteResponse): RemoteResponse {
        if (!response.ok) {
            console.error(response)
        }

        return response
    }
}
