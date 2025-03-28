package io.peekandpoke.aktor

import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.jvm
import de.peekandpoke.ultra.security.jwt.JwtUserData
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.aktor.backend.appuser.AppUser
import io.peekandpoke.aktor.backend.appuser.AppUsersRepo
import io.peekandpoke.aktor.backend.appuser.AppUsersRepo.Companion.asApiModel
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel
import io.peekandpoke.aktor.shared.appuser.model.AppUserRoles
import io.peekandpoke.reaktor.auth.AuthSystem
import io.peekandpoke.reaktor.auth.model.LoginResponse
import io.peekandpoke.reaktor.auth.provider.AuthProvider
import io.peekandpoke.reaktor.auth.provider.AuthProviderFactory
import io.peekandpoke.reaktor.auth.provider.EmailAndPasswordAuthProvider.Companion.emailAndPassword
import io.peekandpoke.reaktor.auth.provider.GoogleSsoAuthProvider.Companion.googleSso
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

class AppUserAuthenticationRealm(
    deps: Lazy<AuthSystem.Deps>,
    appUserRepo: Lazy<AppUsersRepo>,
    factory: Lazy<AuthProviderFactory>,
    keys: Lazy<KeysConfig>,
) : AuthSystem.Realm<AppUser> {
    companion object {
        const val realm = "app-user"
    }

    private val deps by deps
    private val appUserRepo by appUserRepo
    private val factory by factory
    private val keys by keys

    override val id: String = realm

    override val providers: List<AuthProvider> = listOf(
        this.factory.emailAndPassword(),
        this.factory.googleSso(
            googleClientId = this.keys.config.getString("GOOGLE_SSO_CLIENT_ID"),
            googleClientSecret = this.keys.config.getString("GOOGLE_SSO_CLIENT_SECRET"),
        )
    )

    override suspend fun loadUserById(id: String) = appUserRepo.findById(id)

    override suspend fun loadUserByEmail(email: String) = appUserRepo.findByEmail(email)

    override suspend fun generateJwt(user: Stored<AppUser>): LoginResponse.Token {
        val gen = deps.jwtGenerator

        val token = gen.createJwt(
            user = JwtUserData(
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
            withExpiresAt(Kronos.systemUtc.instantNow().plus(1.hours).jvm)
        }

        return LoginResponse.Token(
            token = token,
            permissionsNs = gen.config.permissionsNs,
            userNs = gen.config.userNs,
        )
    }

    override suspend fun serializeUser(user: Stored<AppUser>): JsonObject {
        return Json.encodeToJsonElement(AppUserModel.serializer(), user.asApiModel()).jsonObject
    }
}
