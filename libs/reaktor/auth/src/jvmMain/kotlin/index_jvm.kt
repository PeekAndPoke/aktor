package io.peekandpoke.reaktor.auth

import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.reaktor.auth.api.AuthApiFeature
import io.peekandpoke.reaktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.reaktor.auth.provider.GithubSsoAuth
import io.peekandpoke.reaktor.auth.provider.GoogleSsoAuth

inline val KontainerAware.reaktorAuth: AuthSystem get() = kontainer.get()
inline val ApplicationCall.reaktorAuth: AuthSystem get() = kontainer.reaktorAuth
inline val RoutingContext.reaktorAuth: AuthSystem get() = call.reaktorAuth

val ReaktorAuth = module {
    // Facade
    dynamic(AuthSystem::class)
    dynamic(AuthSystem.Deps::class)
    // Provider Factories
    dynamic(EmailAndPasswordAuth.Factory::class)
    dynamic(GoogleSsoAuth.Factory::class)
    dynamic(GithubSsoAuth.Factory::class)
    // API
    singleton(AuthApiFeature::class)
}
