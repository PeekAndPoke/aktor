package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.api.AuthApiFeature
import de.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import de.peekandpoke.funktor.auth.provider.GithubSsoAuth
import de.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*

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
