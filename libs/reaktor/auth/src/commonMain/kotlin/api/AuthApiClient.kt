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
            body = AuthLoginRequest.serializer(),
            response = AuthLoginResponse.serializer().api(),
        )

        val Update = TypedApiEndpoint.Put(
            uri = "$base/{realm}/update",
            body = AuthUpdateRequest.serializer(),
            response = AuthUpdateResponse.serializer().api(),
        )

        val Recover = TypedApiEndpoint.Put(
            uri = "$base/{realm}/recover",
            body = AuthRecoveryRequest.serializer(),
            response = AuthRecoveryResponse.serializer().api(),
        )

        // Signup
        val Signup = TypedApiEndpoint.Post(
            uri = "$base/{realm}/signup",
            body = AuthSignupRequest.serializer(),
            response = AuthSignupResponse.serializer().api(),
        )

        // Activate
        val Activate = TypedApiEndpoint.Post(
            uri = "$base/{realm}/activate",
            body = AuthActivateRequest.serializer(),
            response = AuthActivateResponse.serializer().api(),
        )
    }

    fun getRealm(): Flow<ApiResponse<AuthRealmModel>> = call(
        GetRealm(
            "realm" to realm,
        )
    )

    fun login(request: AuthLoginRequest): Flow<ApiResponse<AuthLoginResponse>> = call(
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

    fun recover(request: AuthRecoveryRequest): Flow<ApiResponse<AuthRecoveryResponse>> = call(
        Recover(
            "realm" to realm,
            body = request,
        )
    )

    fun signup(request: AuthSignupRequest): Flow<ApiResponse<AuthSignupResponse>> = call(
        Signup(
            "realm" to realm,
            body = request,
        )
    )

    fun activate(request: AuthActivateRequest): Flow<ApiResponse<AuthActivateResponse>> = call(
        Activate(
            "realm" to realm,
            body = request,
        )
    )
}
