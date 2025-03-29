package io.peekandpoke.reaktor.auth.api

import de.peekandpoke.ultra.common.remote.*
import io.peekandpoke.reaktor.auth.model.AuthRealmModel
import io.peekandpoke.reaktor.auth.model.LoginRequest
import io.peekandpoke.reaktor.auth.model.LoginResponse
import kotlinx.coroutines.flow.Flow

class AuthApiClient(private val realm: String, config: Config) : ApiClient(config) {

    companion object {
        const val base = "/auth"

        val GetRealm = TypedApiEndpoint.Get(
            uri = "$base/{realm}/realm",
            response = AuthRealmModel.serializer().api(),
        )

        val Login = TypedApiEndpoint.Post(
            uri = "$base/{realm}/login",
            body = LoginRequest.serializer(),
            response = LoginResponse.serializer().api(),
        )
    }

    fun getRealm(): Flow<ApiResponse<AuthRealmModel>> = call(
        GetRealm("realm" to realm)
    )

    fun login(request: LoginRequest): Flow<ApiResponse<LoginResponse>> = call(
        Login(
            "realm" to realm,
            body = request,
        )
    )
}
