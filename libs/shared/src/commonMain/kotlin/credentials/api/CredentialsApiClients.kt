package io.peekandpoke.aktor.shared.credentials.api

import de.peekandpoke.ultra.common.remote.ApiClient.Config

class CredentialsApiClients(config: Config) {

    val credentials = CredentialsApiClient(config)
}
