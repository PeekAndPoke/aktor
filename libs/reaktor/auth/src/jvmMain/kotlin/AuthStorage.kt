package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.ultra.vault.Stored

interface AuthStorage {
    interface AuthRecordsRepo {
        suspend fun insert(record: AuthRecord): Stored<AuthRecord>

        suspend fun findLatestBy(
            realm: String,
            type: String,
            owner: String,
        ): Stored<AuthRecord>?
    }

    val authRecordsRepo: AuthRecordsRepo
}
