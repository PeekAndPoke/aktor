package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.model.*
import de.peekandpoke.funktor.messaging.MessagingServices
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.security.password.PasswordHasher

class AuthSystem(
    private val realms: List<AuthRealm<Any>>,
) {
    class Deps(
        val messaging: MessagingServices,
        val jwtGenerator: JwtGenerator,
        val storage: AuthStorage,
        val passwordHasher: PasswordHasher,
        val random: AuthRandom,
    )

    init {
        val duplicatedRealms = realms.groupBy { it.id }
            .filter { (id, realms) -> realms.size > 1 }
            .map { (id, realms) -> id }

        if (duplicatedRealms.isNotEmpty()) {
            throw error("Found duplicated authentication realms: $duplicatedRealms")
        }
    }

    fun getRealmOrNull(id: String): AuthRealm<*>? {
        return realms.firstOrNull { it.id == id }
    }

    fun getRealm(id: String): AuthRealm<*> {
        return getRealmOrNull(id) ?: throw AuthError("Realm not found: $id")
    }

    suspend fun login(realm: String, request: AuthLoginRequest): AuthLoginResponse {
        return getRealm(realm).login(request)
    }

    suspend fun update(realm: String, request: AuthUpdateRequest): AuthUpdateResponse {
        return getRealm(realm).update(request)
    }

    suspend fun recover(realm: String, request: AuthRecoveryRequest): AuthRecoveryResponse {
        return getRealm(realm).recover(request)
    }

    // --- Signup / Activate (stubs for now, wired to API) ---
    suspend fun signup(realm: String, request: AuthSignupRequest): AuthSignupResponse {
        // Minimal placeholder; real implementation will be added next
        return AuthSignupResponse.failed
    }

    suspend fun activate(realm: String, request: AuthActivateRequest): AuthActivateResponse {
        // Minimal placeholder; real implementation will be added next
        return AuthActivateResponse(success = false)
    }
}
