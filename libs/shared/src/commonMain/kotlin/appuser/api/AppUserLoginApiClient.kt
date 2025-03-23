package io.peekandpoke.aktor.shared.appuser.api

import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint.Post
import de.peekandpoke.ultra.common.remote.api
import de.peekandpoke.ultra.common.remote.call
import io.peekandpoke.aktor.shared.appuser.model.AppUserLoginResponse
import io.peekandpoke.aktor.shared.model.LoginWithPassword
import kotlinx.coroutines.flow.Flow

class AppUserLoginApiClient(config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/app-user/login"

        val WithPassword = Post(
            uri = "$BASE/password",
            body = LoginWithPassword.serializer(),
            response = AppUserLoginResponse.serializer().api(),
        )
    }

    fun withPassword(body: LoginWithPassword): Flow<ApiResponse<AppUserLoginResponse>> = call(
        WithPassword(
            body = body,
        )
    )
}
