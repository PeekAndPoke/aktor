package io.peekandpoke.reaktor.auth.api

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.core.kronos
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse
import io.peekandpoke.reaktor.auth.AuthError
import io.peekandpoke.reaktor.auth.model.LoginResponse
import io.peekandpoke.reaktor.auth.reaktorAuth
import kotlinx.coroutines.delay
import kotlin.random.Random

class AuthApi(converter: OutgoingConverter) : ApiRoutes("login", converter) {

    val getRealm = AuthApiClient.GetRealm.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Get realm"
        }.codeGen {
            funcName = "getRealm"
        }.authorize {
            public()
        }.handle { params ->

            // Let the bots wait a bit
            val realm = reaktorAuth.getRealm(params.realm)

            ApiResponse.okOrNotFound(
                realm?.asApiModel()
            )
        }
    }

    val login = AuthApiClient.Login.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Login"
        }.codeGen {
            funcName = "login"
        }.authorize {
            public()
        }.handle { params, body ->

            // Let the bots wait a bit
            delay(
                Random(kronos.millisNow()).nextLong(250, 500)
            )

            try {
                val response = reaktorAuth.login(params.realm, body)

                ApiResponse.ok(response)
            } catch (e: AuthError) {
                ApiResponse.forbidden<LoginResponse>().withInfo(e.message ?: "")
            }
        }
    }
}
