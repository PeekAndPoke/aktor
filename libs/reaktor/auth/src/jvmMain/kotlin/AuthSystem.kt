package io.peekandpoke.reaktor.auth

import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.model.AuthRealmModel
import io.peekandpoke.reaktor.auth.model.LoginRequest
import io.peekandpoke.reaktor.auth.model.LoginResponse
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

        fun asApiModel(): AuthRealmModel = AuthRealmModel(
            id = id,
            providers = providers.map { it.asApiModel() }
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

    fun getRealm(id: String): Realm<*>? {
        return realms.firstOrNull { it.id == id }
    }

    suspend fun login(realm: String, request: LoginRequest): LoginResponse {

        val r = realms.firstOrNull { it.id == realm }
            ?: throw AuthError("Realm not found: $realm")

        return r.login(request)
    }
}
