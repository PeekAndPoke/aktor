package io.peekandpoke.aktor.backend.appuser

import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.module
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.aktor.backend.appuser.api.AppUserApiFeature

inline val KontainerAware.appUsers: AppUserServices get() = kontainer.get()
inline val ApplicationCall.appUsers: AppUserServices get() = kontainer.appUsers
inline val RoutingContext.appUsers: AppUserServices get() = call.appUsers

val AppUserModule = module {

    dynamic(AppUserRealm::class)

    singleton(AppUserServices::class)
    singleton(AppUserApiFeature::class)

    singleton(AppUsersRepo::class)
    singleton(AppUsersRepo.Fixtures::class)
}
