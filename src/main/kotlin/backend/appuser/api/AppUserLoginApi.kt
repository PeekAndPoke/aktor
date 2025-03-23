package io.peekandpoke.aktor.backend.appuser.api

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ktorfx.core.kronos
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.datetime.jvm
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.security.jwt.JwtUserData
import de.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.aktor.appUsers
import io.peekandpoke.aktor.backend.appuser.AppUsersRepo.Companion.asApiModel
import io.peekandpoke.aktor.shared.appuser.api.AppUserLoginApiClient
import io.peekandpoke.aktor.shared.appuser.model.AppUserLoginResponse
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel
import io.peekandpoke.aktor.shared.appuser.model.AppUserRoles
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours

class AppUserLoginApi(converter: OutgoingConverter) : ApiRoutes("login", converter) {

    val withPassword = AppUserLoginApiClient.WithPassword.mount {
        docs {
            name = "Log in with password"
        }.codeGen {
            funcName = "withPassword"
        }.authorize {
            public()
        }.handle { body ->

            // Let the bots wait a bit
            delay(
                Random(kronos.millisNow()).nextLong(250, 500)
            )

            val user = appUsers.appUsersRepo.findByEmail(body.user)
                ?: return@handle ApiResponse.forbidden()

            // TODO: check password

            val jwtGenerator = kontainer.get<JwtGenerator>()

            val token = jwtGenerator.createJwt(
                JwtUserData(
                    id = user._id,
                    desc = user.value.name,
                    type = AppUserModel.USER_TYPE,
                    email = user.value.email,
                ),
                permissions = UserPermissions(
                    organisations = setOf(),
                    roles = setOf(AppUserRoles.Default)
                )
            ) {
                // Expires
                withExpiresAt(kronos.instantNow().plus(1.hours).jvm)
            }

            ApiResponse.ok(
                AppUserLoginResponse(
                    token = token,
                    user = user.asApiModel(),
                )
            )
        }
    }
}
