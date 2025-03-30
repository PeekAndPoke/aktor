package io.peekandpoke.reaktor.auth

import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.model.*
import io.peekandpoke.reaktor.auth.provider.AuthProvider
import kotlinx.serialization.json.JsonObject

class AuthSystem(
    private val realms: List<Realm<Any>>,
) {
    class Deps(
        val jwtGenerator: JwtGenerator,
        val storage: AuthStorage,
        val passwordHasher: PasswordHasher,
    )

    interface Realm<USER> {
        /** Unique id of the realm */
        val id: String

        /** Auth providers for this realm */
        val providers: List<AuthProvider>

        /** The password policy for this realm */
        val passwordPolicy: PasswordPolicy get() = PasswordPolicy.default

        suspend fun loadUserById(id: String): Stored<USER>?

        suspend fun loadUserByEmail(email: String): Stored<USER>?

        suspend fun generateJwt(user: Stored<USER>): LoginResponse.Token

        suspend fun serializeUser(user: Stored<USER>): JsonObject

        suspend fun login(request: LoginRequest): LoginResponse {

            val provider = providers.firstOrNull { it.id == request.provider }
                ?: throw AuthError("Provider not found: ${request.provider}")

            val user = provider.login<USER>(realm = this, request = request)
                ?: throw AuthError("User not found")

            val response = LoginResponse(
                token = generateJwt(user),
                realm = asApiModel(),
                user = serializeUser(user),
            )

            return response
        }

        suspend fun update(request: AuthUpdateRequest): AuthUpdateResponse {
            val provider = providers.firstOrNull { it.id == request.provider }
                ?: throw AuthError("Provider not found: ${request.provider}")

            val user = loadUserById(request.userId)
                ?: throw AuthError("User not found: ${request.userId}")

            val result = provider.update(realm = this, user = user, request = request)

            return result
        }

        fun asApiModel(): AuthRealmModel = AuthRealmModel(
            id = id,
            providers = providers.map { it.asApiModel() },
            passwordPolicy = passwordPolicy,
        )
    }

    init {
        val duplicatedRealms = realms.groupBy { it.id }
            .filter { (id, realms) -> realms.size > 1 }
            .map { (id, realms) -> id }

        if (duplicatedRealms.isNotEmpty()) {
            throw error("Found duplicated authentication realms: $duplicatedRealms")
        }
    }

    fun getRealmOrNull(id: String): Realm<*>? {
        return realms.firstOrNull { it.id == id }
    }

    fun getRealm(id: String): Realm<*> {
        return getRealmOrNull(id) ?: throw AuthError("Realm not found: $id")
    }

    suspend fun login(realm: String, request: LoginRequest): LoginResponse {
        return getRealm(realm).login(request)
    }

    suspend fun update(realm: String, request: AuthUpdateRequest): AuthUpdateResponse {
        return getRealm(realm).update(request)
    }
}
