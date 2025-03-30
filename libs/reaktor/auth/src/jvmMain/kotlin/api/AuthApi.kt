package io.peekandpoke.reaktor.auth.api

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.core.user
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse
import io.peekandpoke.reaktor.auth.AuthError
import io.peekandpoke.reaktor.auth.model.AuthUpdateResponse
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
            val realm = reaktorAuth.getRealmOrNull(params.realm)

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
            delay(Random.nextLong(250, 500))

            try {
                reaktorAuth
                    .login(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.forbidden<LoginResponse>().withInfo(e.message ?: "")
            }
        }
    }

    val update = AuthApiClient.Update.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Update"
        }.codeGen {
            funcName = "update"
        }.authorize {
            public()
        }.handle { params, body ->
            // Let the bots wait a bit
            delay(Random.nextLong(250, 500))

            // Check if the current user is able to do the update
            if (user.record.userId != body.userId) {
                return@handle ApiResponse.forbidden<AuthUpdateResponse>()
            }

            try {
                reaktorAuth
                    .update(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.forbidden<AuthUpdateResponse>().withInfo(e.message ?: "")
            }
        }
    }
}
