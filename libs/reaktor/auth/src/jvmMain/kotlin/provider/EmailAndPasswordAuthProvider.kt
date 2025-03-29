package io.peekandpoke.reaktor.auth.provider

import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.AuthError
import io.peekandpoke.reaktor.auth.AuthSystem
import io.peekandpoke.reaktor.auth.domain.AuthRecord
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.LoginRequest

class EmailAndPasswordAuthProvider(
    override val id: String,
    deps: Lazy<AuthSystem.Deps>,
) : AuthProvider {

    companion object {
        fun AuthProviderFactory.emailAndPassword(
            id: String = "email-password",
        ) = EmailAndPasswordAuthProvider(id = id, deps = deps)
    }

    private val deps by deps

    override suspend fun <USER> login(realm: AuthSystem.Realm<USER>, request: LoginRequest): Stored<USER> {

        var typed = (request as? LoginRequest.EmailAndPassword)
            ?: throw AuthError("Invalid credentials")

        val email = typed.email
        val password = typed.password

        if (email.isBlank() || password.isBlank()) {
            throw AuthError("Invalid credentials")
        }

        val user = realm.loadUserByEmail(email)
            ?: throw AuthError("Invalid credentials")

        val record = deps.storage.authRecordsRepo.findLatestBy(
            realm = realm.id,
            type = AuthRecord.Entry.Password.serialName,
            owner = user._id,
        )

        record ?: throw AuthError("Invalid credentials")

        val entry = record.value.entry as? AuthRecord.Entry.Password

        entry ?: throw AuthError("Invalid credentials")

        if (deps.passwordHasher.check(plaintext = password, hash = entry.hash)) {
            return user
        }

        throw AuthError("Invalid password")
    }

    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            type = AuthProviderModel.TYPE_EMAIL_PASSWORD,
        )
    }
}
