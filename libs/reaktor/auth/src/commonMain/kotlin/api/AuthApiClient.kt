package io.peekandpoke.reaktor.auth.api

import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint
import de.peekandpoke.ultra.common.remote.api
import de.peekandpoke.ultra.common.remote.call
import io.peekandpoke.reaktor.auth.model.LoginRequest
import io.peekandpoke.reaktor.auth.model.LoginResponse

class AuthApiClient(private val realm: String, config: Config) : ApiClient(config) {

    companion object {
        const val base = "/auth"

        val Login = TypedApiEndpoint.Post(
            uri = "$base/{realm}/login",
            body = LoginRequest.serializer(),
            response = LoginResponse.serializer().api(),
        )
    }

    fun login(request: LoginRequest) = call(
        Login(
            "realm" to realm,
            body = request,
        )
    )
}
