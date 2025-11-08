package io.peekandpoke.aktor.backend.credentials.db

import de.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.hooks.TimestampedHook

class UserCredentialsRepo(
    driver: KarangoDriver,
    timestamps: TimestampedHook,
) : EntityRepository<UserCredentials>(
    name = "user_credentials",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks.of(
        timestamps.onBeforeSave(),
    )
) {
    class Fixtures(
        repo: UserCredentialsRepo,
    ) : RepoFixtureLoader<UserCredentials>(repo = repo)

    override fun IndexBuilder<UserCredentials>.buildIndexes() {
        persistentIndex {
            field { userId }
        }
    }

    suspend fun findByUserId(user: String) = find {
        FOR(repo) { credentials ->
            FILTER(credentials.userId EQ user)
            RETURN(credentials)
        }
    }
}
