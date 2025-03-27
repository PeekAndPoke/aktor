package io.peekandpoke.reaktor.auth.api

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiFeature
import de.peekandpoke.ktorfx.rest.ApiRoutes

class AuthApiFeature(converter: OutgoingConverter) : ApiFeature {

    data class RealmParam(
        val realm: String,
    )

    override val name = "Auth"

    override val description = """
        Endpoints for authentication.
    """.trimIndent()

    val auth = AuthApi(converter)

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        auth,
    )
}
