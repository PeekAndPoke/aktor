package io.peekandpoke.aktor.app

import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.core.fixtures.RepoFixtureLoader
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.hooks.TimestampedHook

class AppUserRepo(
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
    class Fixtures(
        repo: AppUserRepo,
    ) : RepoFixtureLoader<AppUser>(repo = repo)

    interface OnAfterSave : Repository.Hooks.OnAfterSave<AppUser>
}
