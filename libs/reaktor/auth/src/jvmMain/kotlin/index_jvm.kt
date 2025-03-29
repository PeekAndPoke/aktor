package io.peekandpoke.reaktor.auth

import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.reaktor.auth.api.AuthApiFeature
import io.peekandpoke.reaktor.auth.provider.EmailAndPasswordAuthProvider

inline val KontainerAware.reaktorAuth: AuthSystem get() = kontainer.get()
inline val ApplicationCall.reaktorAuth: AuthSystem get() = kontainer.reaktorAuth
inline val RoutingContext.reaktorAuth: AuthSystem get() = call.reaktorAuth

val ReaktorAuth = module {

    dynamic(AuthSystem::class)
    dynamic(AuthSystem.Deps::class)

    // Providers
    dynamic(EmailAndPasswordAuthProvider::class)

    // API
    singleton(AuthApiFeature::class)
}
