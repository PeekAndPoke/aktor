package io.peekandpoke.aktor.backend.llms.api

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse
import io.peekandpoke.aktor.llms
import io.peekandpoke.aktor.shared.llms.api.LlmsApiClient

class LlmsApi(converter: OutgoingConverter) : ApiRoutes("login", converter) {

    val list = LlmsApiClient.List.mount {
        docs {
            name = "List"
        }.codeGen {
            funcName = "list"
        }.authorize {
            public()
        }.handle {
            ApiResponse.ok(
                llms.registry.getAll().map { it.asApiModel() }
            )
        }
    }
}
