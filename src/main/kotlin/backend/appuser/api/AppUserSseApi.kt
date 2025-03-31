package io.peekandpoke.aktor.backend.appuser.api

import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import io.ktor.server.sse.*
import io.peekandpoke.aktor.api.SseSessions
import io.peekandpoke.aktor.shared.appuser.api.AppUserSseApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AppUserSseApi(converter: OutgoingConverter) : ApiRoutes("sse", converter) {

    val connect = AppUserSseApiClient.Companion.Connect.mount(AppUserApiFeature.AppUserParam::class) {
        docs {
            name = "Connect"
        }.codeGen {
            funcName = "connect"
        }.authorize {
            public()
        }.handle { params ->

            val session = this

            println("Starting sse session for ${params.user}")

            SseSessions.session = session

            session.heartbeat()

            while (session.isActive == true) {
                delay(100)
            }
        }
    }
}
