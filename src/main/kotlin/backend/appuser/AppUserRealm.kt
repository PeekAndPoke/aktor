package io.peekandpoke.aktor.backend.appuser

import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.AuthSystem
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import de.peekandpoke.funktor.auth.provider.GithubSsoAuth
import de.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import de.peekandpoke.funktor.messaging.Email
import de.peekandpoke.funktor.messaging.api.EmailBody
import de.peekandpoke.funktor.messaging.api.EmailDestination
import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.jvm
import de.peekandpoke.ultra.security.jwt.JwtUserData
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.vault.Stored
import io.ktor.server.config.*
import io.peekandpoke.aktor.KeysConfig
import io.peekandpoke.aktor.backend.appuser.AppUsersRepo.Companion.asApiModel
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel
import io.peekandpoke.aktor.shared.appuser.model.AppUserRoles
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.hours

class AppUserRealm(
    deps: Lazy<AuthSystem.Deps>,
    appUserRepo: Lazy<AppUsersRepo>,
    keys: Lazy<KeysConfig>,
    emailAndPassword: Lazy<EmailAndPasswordAuth.Factory>,
    googleSso: Lazy<GoogleSsoAuth.Factory>,
    githubSso: Lazy<GithubSsoAuth.Factory>,
) : AuthRealm<AppUser> {
    companion object {
        const val realm = "app-user"
    }

    private inner class MessagingImpl : AuthRealm.Messaging<AppUser> {
        override suspend fun sendPasswordChangedEmail(user: Stored<AppUser>): EmailResult {
            return deps.messaging.mailing.send(
                Email(
                    // TODO: configure default sender
                    source = "treore@jointhebase.co",
                    destination = EmailDestination.to(getUserEmail(user)),
                    subject = "Password changed",
                    body = EmailBody.Html {
                        body {
                            h1 { +"Heads up!" }

                            p {
                                +"Your password was changed. If this was not you, please contact us!"
                            }

                            p {
                                +"Yours sincerely,"
                                br()
                                +"Treore Xnefgra"
                            }
                        }
                    }
                )
            )
        }
    }

    private val deps by deps
    private val appUserRepo by appUserRepo
    private val keys by keys
    private val emailAndPassword by emailAndPassword
    private val googleSso by googleSso
    private val githubSso by githubSso

    override val id: String = realm

    override val messaging: AuthRealm.Messaging<AppUser> = MessagingImpl()

    override val providers = listOfNotNull(
        // Email / Password
        this.emailAndPassword(
            capabilities = setOf(
                AuthProviderModel.Capability.SignIn,
                AuthProviderModel.Capability.SignUp,
            )
        ),
        // Google SSO
        this.keys.config.tryGetString("GOOGLE_SSO_CLIENT_ID")?.let { clientId ->
            googleSso(
                googleClientId = clientId,
                capabilities = setOf(
                    AuthProviderModel.Capability.SignIn,
                    AuthProviderModel.Capability.SignUp,
                )
            )
        },
        // Github SSO
        this.keys.config.tryGetString("GITHUB_SSO_CLIENT_ID")?.let { clientId ->
            this.keys.config.tryGetString("GITHUB_SSO_CLIENT_SECRET")?.let { secret ->
                githubSso(
                    githubClientId = clientId,
                    githubClientSecret = secret,
                    capabilities = setOf(
                        AuthProviderModel.Capability.SignIn,
                        AuthProviderModel.Capability.SignUp,
                    )
                )
            }
        },
    )

    override suspend fun loadUserById(id: String) = appUserRepo.findById(id)

    override suspend fun loadUserByEmail(email: String) = appUserRepo.findByEmail(email)

    override suspend fun generateJwt(user: Stored<AppUser>): AuthSignInResponse.Token {
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

        return AuthSignInResponse.Token(
            token = token,
            permissionsNs = gen.config.permissionsNs,
            userNs = gen.config.userNs,
        )
    }

    override suspend fun getUserEmail(user: Stored<AppUser>): String {
        return user.value.email
    }

    override suspend fun serializeUser(user: Stored<AppUser>): JsonObject {
        return Json.encodeToJsonElement(
            AppUserModel.serializer(), user.asApiModel()
        ).jsonObject
    }

    override suspend fun createUserForSignup(email: String, displayName: String?): Stored<AppUser> {
        val name = (displayName ?: "").trim()
        return appUserRepo.insert(
            AppUser(
                name = name,
                email = email.trim(),
            )
        )
    }
}
