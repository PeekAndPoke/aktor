package io.peekandpoke.aktor.backend.credentials

import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.aktor.backend.credentials.api.CredentialsApiFeature
import io.peekandpoke.aktor.backend.credentials.db.UserCredentialsRepo
import io.peekandpoke.aktor.backend.credentials.services.GoogleOAuthService

inline val KontainerAware.credentials: CredentialsServices get() = kontainer.get()
inline val ApplicationCall.credentials: CredentialsServices get() = kontainer.credentials
inline val RoutingContext.credentials: CredentialsServices get() = call.credentials

data class CredentialsConfig(
    val googleClientId: String? = null,
    val googleClientSecret: String? = null,
    val googleClientAppName: String? = null,
)

val CredentialsModule = module { config: CredentialsConfig ->
    singleton(CredentialsServices::class)
    singleton(CredentialsApiFeature::class)

    // Services
    config.googleClientId?.let { clientId ->
        config.googleClientSecret?.let { clientSecret ->
            config.googleClientAppName?.let { appName ->

                singleton(GoogleOAuthService::class) { userCredentialsRepo: Lazy<UserCredentialsRepo> ->
                    GoogleOAuthService(
                        clientId = clientId,
                        clientSecret = clientSecret,
                        appName = appName,
                        userCredentialsRepo = userCredentialsRepo,
                    )
                }
            }
        }
    }

    // Database
    dynamic(UserCredentialsRepo::class)
    dynamic(UserCredentialsRepo.Fixtures::class)
}
