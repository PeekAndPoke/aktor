package io.peekandpoke.reaktor.auth.provider

import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.AuthError
import io.peekandpoke.reaktor.auth.AuthSystem
import io.peekandpoke.reaktor.auth.domain.AuthProvider
import io.peekandpoke.reaktor.auth.domain.AuthRecord

class AuthWithEmailAndPasswordProvider(
    deps: Lazy<AuthSystem.Deps>,
) : AuthProvider {

    private val deps by deps

    /**
     * Tries to find a user with the [email] and checks for the [password] to be valid.
     *
     * The user will be return, when it is found and the password is correct.
     *
     * Otherwise [AuthError] will be thrown.
     */
    suspend fun <USER> login(realm: AuthSystem.Realm<USER>, email: String, password: String): Stored<USER> {

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
}
