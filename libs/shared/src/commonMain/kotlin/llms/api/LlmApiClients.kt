package io.peekandpoke.aktor.shared.llms.api

import de.peekandpoke.ultra.common.remote.ApiClient.Config

class LlmApiClients(config: Config) {
    val llms = LlmsApiClient(config)
}
