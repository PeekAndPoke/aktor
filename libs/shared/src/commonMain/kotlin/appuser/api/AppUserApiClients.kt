package io.peekandpoke.aktor.shared.appuser.api

import de.peekandpoke.ultra.common.remote.ApiClient.Config

class AppUserApiClients(config: Config) {
    val sse = AppUserSseApiClient(config)
    val conversations = AppUserConversationsApiClient(config)
}
