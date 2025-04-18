package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.AuthSystem
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.auth.findLatestRecordBy
import de.peekandpoke.funktor.auth.model.*
import de.peekandpoke.ultra.logging.Log
import de.peekandpoke.ultra.vault.Stored


/**
 * Authentication provider for handling email and password-based authentication.
 *
 * This class provides methods for user login and user account updates, such as
 * changing passwords. It validates the user's credentials against the stored
 * records while adhering to the realm's password policy.
 *
 * @param id A unique identifier for the provider.
 * @param deps Lazily loaded dependencies required for authentication operations.
 */
class EmailAndPasswordAuth(
    override val id: String,
    private val log: Log,
    deps: Lazy<AuthSystem.Deps>,
) : AuthProvider {
    /**
     * Factory for creating instances of the EmailAndPasswordAuth provider.
     *
     * This factory allows the configuration of authentication providers with a default
     * or custom identifier. It streamlines the process of instantiating the provider
     * with the required dependencies.
     *
     * @param deps Lazily loaded dependencies required for creating instances of the provider.
     */
    class Factory(
        private val deps: Lazy<AuthSystem.Deps>,
        private val log: Log,
    ) {
        operator fun invoke(id: String = "email-password") = EmailAndPasswordAuth(
            id = id,
            deps = deps,
            log = log,
        )
    }

    private val deps by deps

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER> login(realm: AuthRealm<USER>, request: AuthLoginRequest): Stored<USER> {

        var typed: AuthLoginRequest.EmailAndPassword = (request as? AuthLoginRequest.EmailAndPassword)
            ?: throw AuthError.invalidCredentials()

        val email = typed.email.takeIf { it.isNotBlank() }
            ?: throw AuthError.invalidCredentials()

        val password = typed.password.takeIf { it.isNotBlank() }
            ?: throw AuthError.invalidCredentials()

        val user = realm.loadUserByEmail(email)
            ?: throw AuthError.invalidCredentials()

        validateCurrentPassword(realm, user, password).takeIf { it == true }
            ?: throw AuthError.invalidCredentials()

        return user
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER> update(
        realm: AuthRealm<USER>,
        user: Stored<USER>,
        request: AuthUpdateRequest,
    ): AuthUpdateResponse {

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val result = when (request) {
            is AuthUpdateRequest.SetPassword -> {
                // 1. Check for new password to meet the password policy
                realm.passwordPolicy.matches(request.newPassword).takeIf { it == true }
                    ?: throw AuthError.weekPassword()

                // 3. Write new password entry into database
                deps.storage.createRecord {
                    AuthRecord.Password(
                        realm = realm.id,
                        ownerId = user._id,
                        hash = deps.passwordHasher.hash(request.newPassword),
                    )
                }

                val emailResult = realm.messaging.sendPasswordChangedEmail(user)

                if (emailResult.success == false) {
                    log.warning("Sending 'Password Changed' failed for user ${user._id} ${realm.getUserEmail(user)}")
                }

                AuthUpdateResponse(success = true)
            }

            else -> AuthUpdateResponse(success = false)
        }

        return result
    }

    override suspend fun <USER> recover(
        realm: AuthRealm<USER>,
        request: AuthRecoveryRequest,
    ): AuthRecoveryResponse {

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        return when (request) {
            is AuthRecoveryRequest.ResetPassword -> {

                val user = realm.loadUserByEmail(request.email)
                    ?: return AuthRecoveryResponse.failed

                val token = deps.random.getToken()

                // TODO: write recovery token into db
                // TODO: send email with recovery token
                // TODO: build ui-endpoint to handle the token
                // TODO: add additional LoginRequest.WithOnetimeToken()
                // TODO: handle this one above in the login method

                AuthRecoveryResponse.success
            }

            else -> AuthRecoveryResponse.failed
        }

    }

    private suspend fun <USER> validateCurrentPassword(
        realm: AuthRealm<USER>,
        user: Stored<USER>,
        password: String,
    ): Boolean {
        val record = deps.storage.findLatestRecordBy(
            type = AuthRecord.Password,
            realm = realm.id,
            owner = user._id,
        ) ?: return false

        return deps.passwordHasher.check(plaintext = password, hash = record.value.hash)
    }

    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            type = AuthProviderModel.TYPE_EMAIL_PASSWORD,
        )
    }
}
