package de.peekandpoke.funktor.auth.api

import de.peekandpoke.funktor.auth.model.*
import de.peekandpoke.ultra.common.remote.*
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

        val Update = TypedApiEndpoint.Put(
            uri = "$base/{realm}/update",
            body = AuthUpdateRequest.serializer(),
            response = AuthUpdateResponse.serializer().api(),
        )
    }

    fun getRealm(): Flow<ApiResponse<AuthRealmModel>> = call(
        GetRealm(
            "realm" to realm,
        )
    )

    fun login(request: LoginRequest): Flow<ApiResponse<LoginResponse>> = call(
        Login(
            "realm" to realm,
            body = request,
        )
    )

    fun update(request: AuthUpdateRequest): Flow<ApiResponse<AuthUpdateResponse>> = call(
        Update(
            "realm" to realm,
            body = request,
        )
    )
}
