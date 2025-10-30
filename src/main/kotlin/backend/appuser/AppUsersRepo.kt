package io.peekandpoke.aktor.backend.appuser

import de.peekandpoke.funktor.auth.AuthRecordStorage
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel

class AppUsersRepo(
    driver: KarangoDriver,
    onAfterSaves: List<OnAfterSave>,
    timestamps: TimestampedHook,
) : EntityRepository<AppUser>(
    name = "app_users",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamps.onBeforeSave())
) {
    companion object {
        fun Storable<AppUser>.asApiModel() = with(value) {
            AppUserModel(
                id = _id,
                name = name,
                email = email,
            )
        }
    }

    class Fixtures(
        repo: AppUsersRepo,
        private val authRecords: AuthRecordStorage,
        private val passwordHasher: PasswordHasher,
    ) : RepoFixtureLoader<AppUser>(repo = repo) {

        private val commonPassword = "S3cret123!"

        private suspend fun Stored<AppUser>.createPassword(password: String = commonPassword) {
            authRecords.createRecord {
                AuthRecord.Password(
                    realm = AppUserRealm.realm,
                    ownerId = _id,
                    hash = passwordHasher.hash(password)
                )
            }
        }

        val karsten = singleFix {
            repo.insert(
                "karsten", AppUser(
                    name = "Karsten",
                    email = "karsten.john.gerber@googlemail.com",
                )
            ).also { user -> user.createPassword() }
        }
    }

    interface OnAfterSave : Repository.Hooks.OnAfterSave<AppUser>

    override fun IndexBuilder<AppUser>.buildIndexes() {
        persistentIndex {
            field { email }

            options {
                unique(true)
            }
        }
    }

    suspend fun findByEmail(email: String) = findFirst {
        FOR(repo) { user ->
            FILTER(user.email EQ email)

            LIMIT(1)

            RETURN(user)
        }
    }
}
