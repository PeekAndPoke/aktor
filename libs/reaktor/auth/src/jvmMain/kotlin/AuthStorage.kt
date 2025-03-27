package io.peekandpoke.reaktor.auth

import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.domain.AuthRecord

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
