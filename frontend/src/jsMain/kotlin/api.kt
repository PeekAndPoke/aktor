package de.peekandpoke.aktor.frontend

import de.peekandpoke.ultra.common.remote.ApiClient.Config
import de.peekandpoke.ultra.common.remote.RemoteRequest
import de.peekandpoke.ultra.common.remote.RemoteResponse
import de.peekandpoke.ultra.common.remote.RequestInterceptor
import de.peekandpoke.ultra.common.remote.ResponseInterceptor
import io.peekandpoke.aktor.shared.api.AppUserLoginApiClient
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
        )
    )

    val login = AppUserLoginApiClient(config)
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
