package io.peekandpoke.reaktor.auth.provider

import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.AuthError
import io.peekandpoke.reaktor.auth.AuthSystem
import io.peekandpoke.reaktor.auth.domain.AuthRecord
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.AuthUpdateRequest
import io.peekandpoke.reaktor.auth.model.AuthUpdateResponse
import io.peekandpoke.reaktor.auth.model.LoginRequest


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
    class Factory(private val deps: Lazy<AuthSystem.Deps>) {
        operator fun invoke(id: String = "email-password") = EmailAndPasswordAuth(
            id = id,
            deps = deps,
        )
    }

    private val deps by deps

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER> login(realm: AuthSystem.Realm<USER>, request: LoginRequest): Stored<USER> {

        var typed: LoginRequest.EmailAndPassword = (request as? LoginRequest.EmailAndPassword)
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
        realm: AuthSystem.Realm<USER>,
        user: Stored<USER>,
        request: AuthUpdateRequest,
    ): AuthUpdateResponse {

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val result = when (request) {
            is AuthUpdateRequest.SetPassword -> {
                // 1. Check for new password to meet the password policy
                realm.passwordPolicy.matches(request.newPassword).takeIf { it == true }
                    ?: throw AuthError.weekPassword()

                // 2. Check for old password to be correct
                validateCurrentPassword(realm, user, request.oldPassword).takeIf { it == true }
                    ?: throw AuthError.invalidCredentials()

                // 3. Write new password entry into database
                deps.storage.authRecordsRepo.insert(
                    AuthRecord(
                        realm = realm.id,
                        ownerId = user._id,
                        entry = AuthRecord.Entry.Password(
                            hash = deps.passwordHasher.hash(request.newPassword)
                        ),
                        expiresAt = null,
                    )
                )

                AuthUpdateResponse(success = true)
            }

            else -> AuthUpdateResponse(success = false)
        }

        return result
    }

    private suspend fun <USER> validateCurrentPassword(
        realm: AuthSystem.Realm<USER>,
        user: Stored<USER>,
        password: String,
    ): Boolean {
        val record = deps.storage.authRecordsRepo.findLatestBy(
            realm = realm.id,
            type = AuthRecord.Entry.Password.serialName,
            owner = user._id,
        ) ?: return false

        val entry = (record.value.entry as? AuthRecord.Entry.Password)
            ?: return false

        return deps.passwordHasher.check(plaintext = password, hash = entry.hash)
    }

    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            type = AuthProviderModel.TYPE_EMAIL_PASSWORD,
        )
    }
}
