package de.peekandpoke.funktor.auth.db.karango

import de.peekandpoke.funktor.auth.AuthStorage
import de.peekandpoke.funktor.auth.domain.*
import de.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import de.peekandpoke.karango.aql.DESC
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import de.peekandpoke.ultra.vault.slumber._type
import de.peekandpoke.ultra.vault.slumber.ts

class KarangoAuthRecordsRepo(
    driver: KarangoDriver,
    onAfterSaves: List<OnAfterSave>,
    timestamps: TimestampedHook,
) : AuthStorage.AuthRecordsRepo, EntityRepository<AuthRecord>(
    name = "system_auth_records",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamps.onBeforeSave())
) {
    companion object {
//        fun Storable<AuthRecord>.asApiModel() = with(value) {
//            AppUserModel(
//                id = _id,
//                name = name,
//                email = email,
//            )
//        }
    }

    class Fixtures(
        repo: KarangoAuthRecordsRepo,
    ) : RepoFixtureLoader<AuthRecord>(repo = repo)

    interface OnAfterSave : Repository.Hooks.OnAfterSave<AuthRecord>

    override fun IndexBuilder<AuthRecord>.buildIndexes() {
        persistentIndex {
            field { realm }
            field { entry._type }
            field { ownerId }
        }

        ttlIndex {
            field { expiresAt }
        }
    }

    override suspend fun insert(record: AuthRecord): Stored<AuthRecord> {
        return super.insert(record)
    }

    override suspend fun findLatestBy(realm: String, type: String, owner: String): Stored<AuthRecord>? {
        return findFirst {
            FOR(repo) { r ->
                FILTER(r.realm EQ realm)
                FILTER(r.entry._type EQ type)
                FILTER(r.ownerId EQ owner)

                SORT(r.createdAt.ts.DESC)

                LIMIT(1)

                RETURN(r)
            }
        }
    }
}
