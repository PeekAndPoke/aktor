package io.peekandpoke.aktor.backend.llms.api

import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
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
