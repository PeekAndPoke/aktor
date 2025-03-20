package io.peekandpoke.aktor.backend

import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.core.fixtures.RepoFixtureLoader
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.peekandpoke.aktor.shared.model.AppUserModel

class AppUsersRepo(
    driver: KarangoDriver,
    onAfterSaves: List<OnAfterSave>,
    timestamps: TimestampedHook,
) : EntityRepository<AppUser>(
    name = "app_users",
    storedType = kType<AppUser>(),
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
    ) : RepoFixtureLoader<AppUser>(repo = repo) {

        val karsten = fix {
            "karsten" to AppUser(
                name = "Karsten",
                email = "karsten.john.gerber@googlemail.com",
            )
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
