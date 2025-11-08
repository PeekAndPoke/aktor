package io.peekandpoke.aktor.backend.credentials.api

import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiFeature
import de.peekandpoke.funktor.rest.ApiRoutes

class CredentialsApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "Credentials"

    override val description = """
        Exposes api endpoints for managing credentials.
    """.trimIndent()

    val credentialsApi = CredentialsApi(converter)

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        credentialsApi,
    )
}
