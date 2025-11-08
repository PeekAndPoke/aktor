package de.peekandpoke.aktor.frontend

import de.peekandpoke.funktor.auth.api.AuthApiClient
import de.peekandpoke.ultra.common.remote.ApiClient.Config
import de.peekandpoke.ultra.common.remote.ErrorLoggingResponseInterceptor
import de.peekandpoke.ultra.common.remote.SetBearerRequestInterceptor
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import io.peekandpoke.aktor.shared.appuser.api.AppUserApiClients
import io.peekandpoke.aktor.shared.credentials.api.CredentialsApiClients
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
            SetBearerRequestInterceptor(tokenProvider)
        ),
        responseInterceptors = listOf(
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
    val credentials = CredentialsApiClients(config)
    val llms = LlmApiClients(config)
}
