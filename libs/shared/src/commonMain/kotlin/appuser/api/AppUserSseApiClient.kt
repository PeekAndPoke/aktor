package io.peekandpoke.aktor.shared.appuser.api

import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint
import de.peekandpoke.ultra.common.remote.call
import io.ktor.client.plugins.sse.*

class AppUserSseApiClient(config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/app-user/{user}/sse"

        val Connect = TypedApiEndpoint.Sse(
            uri = "$BASE/connect",
        )
    }

    suspend fun connect(user: String): ClientSSESession = call(
        Connect(
            "user" to user,
        )
    )
}
