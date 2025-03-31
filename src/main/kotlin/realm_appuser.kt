package io.peekandpoke.aktor

import de.peekandpoke.funktor.auth.AuthSystem
import de.peekandpoke.funktor.auth.model.LoginResponse
import de.peekandpoke.funktor.auth.provider.AuthProvider
import de.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import de.peekandpoke.funktor.auth.provider.GithubSsoAuth
import de.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.jvm
import de.peekandpoke.ultra.security.jwt.JwtUserData
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.vault.Stored
import io.ktor.server.config.*
import io.peekandpoke.aktor.backend.appuser.AppUser
import io.peekandpoke.aktor.backend.appuser.AppUsersRepo
import io.peekandpoke.aktor.backend.appuser.AppUsersRepo.Companion.asApiModel
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel
import io.peekandpoke.aktor.shared.appuser.model.AppUserRoles
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

class AppUserAuthenticationRealm(
    deps: Lazy<AuthSystem.Deps>,
    appUserRepo: Lazy<AppUsersRepo>,
    keys: Lazy<KeysConfig>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
    googleSso: Lazy<GoogleSsoAuth.Factory>,
    githubSso: Lazy<GithubSsoAuth.Factory>,
) : AuthSystem.Realm<AppUser> {
    companion object {
        const val realm = "app-user"
    }

    private val deps by deps
    private val appUserRepo by appUserRepo
    private val keys by keys
    private val emailAndPassword by emailAndPassword
    private val googleSso by googleSso
    private val githubSso by githubSso

    override val id: String = realm

    override val providers: List<AuthProvider> = listOfNotNull(
        // Email / Password
        this.emailAndPassword(),
        // Google SSO
        this.keys.config.tryGetString("GOOGLE_SSO_CLIENT_ID")?.let { googleSso(googleClientId = it) },
        // Github SSO
        this.keys.config.tryGetString("GITHUB_SSO_CLIENT_ID")?.let { clientId ->
            this.keys.config.tryGetString("GITHUB_SSO_CLIENT_SECRET")?.let { secret ->
                githubSso(githubClientId = clientId, githubClientSecret = secret)
            }
        },
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
