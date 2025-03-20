package io.peekandpoke.aktor.api

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiFeature
import de.peekandpoke.ktorfx.rest.ApiRoutes

class AppUserApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "AppUsers"

    override val description = """
        Exposes api endpoints for managing app users.
    """.trimIndent()

    val loginApi = AppUserLoginApi(converter)

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        loginApi,
    )
}
