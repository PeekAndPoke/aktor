package de.peekandpoke.funktor.auth.api

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.model.AuthLoginResponse
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthUpdateResponse
import de.peekandpoke.funktor.auth.reaktorAuth
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.core.user
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse
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
            letTheBotsWait()

            try {
                reaktorAuth
                    .login(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.forbidden<AuthLoginResponse>().withInfo(e.message ?: "")
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
            letTheBotsWait()

            // Check if the current user is able to do the update
            if (user.record.userId != body.userId) {
                return@handle ApiResponse.forbidden<AuthUpdateResponse>()
            }

            try {
                reaktorAuth
                    .update(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest<AuthUpdateResponse>(
                    AuthUpdateResponse.failed
                ).withInfo(e.message ?: "")
            }
        }
    }

    val recover = AuthApiClient.Recover.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Recover"
        }.codeGen {
            funcName = "recover"
        }.authorize {
            public()
        }.handle { params, body ->
            // Let the bots wait a bit
            letTheBotsWait()

            try {
                reaktorAuth
                    .recover(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest<AuthRecoveryResponse>(
                    AuthRecoveryResponse.failed
                ).withInfo(e.message ?: "")
            }
        }
    }

    private suspend fun letTheBotsWait() {
        delay(Random.nextLong(250, 500))
    }
}
