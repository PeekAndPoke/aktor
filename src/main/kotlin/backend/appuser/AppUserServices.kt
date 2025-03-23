package io.peekandpoke.aktor.backend.appuser

class AppUserServices(
    appUsersRepo: Lazy<AppUsersRepo>,
) {
    val appUsersRepo by appUsersRepo
}
