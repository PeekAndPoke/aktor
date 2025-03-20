package io.peekandpoke.aktor.backend

class AppUserServices(
    appUsersRepo: Lazy<AppUsersRepo>,
) {
    val appUsersRepo by appUsersRepo
}
