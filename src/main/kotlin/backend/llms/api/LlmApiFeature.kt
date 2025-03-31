package io.peekandpoke.aktor.backend.llms.api

import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiFeature
import de.peekandpoke.funktor.rest.ApiRoutes

class LlmApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "Llm"

    override val description = """
        Exposes api endpoints for managing LLMs.
    """.trimIndent()

    val llmsApi = LlmsApi(converter)

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        llmsApi,
    )
}
