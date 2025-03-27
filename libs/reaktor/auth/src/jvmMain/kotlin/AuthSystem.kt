package io.peekandpoke.reaktor.auth

import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import de.peekandpoke.ultra.security.password.PasswordHasher
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.domain.AuthProvider
import io.peekandpoke.reaktor.auth.model.LoginRequest
import io.peekandpoke.reaktor.auth.model.LoginResponse
import io.peekandpoke.reaktor.auth.provider.AuthWithEmailAndPasswordProvider
import kotlinx.serialization.json.JsonObject

class AuthSystem(
    private val deps: Deps,
    private val realms: List<Realm<Any>>,
) {
    class Deps(
        val jwtGenerator: JwtGenerator,
        val kronos: Kronos,
        val storage: AuthStorage,
        val passwordHasher: PasswordHasher,
    )

    class AuthCtx(
        val auth: AuthSystem,
        val realm: Realm<Any>,
    )

    interface Realm<USER> {
        val id: String

        val providers: List<AuthProvider>

        suspend fun loadUserById(id: String): Stored<USER>?

        suspend fun loadUserByEmail(email: String): Stored<USER>?

        suspend fun generateJwt(user: Stored<USER>): String

        suspend fun serializeUser(user: Stored<USER>): JsonObject

        suspend fun login(request: LoginRequest): LoginResponse {
            val user = when (request) {
                is LoginRequest.EmailAndPassword -> {
                    val provider = providers
                        .filterIsInstance<AuthWithEmailAndPasswordProvider>().firstOrNull()
                        ?: throw AuthError("Email and password authentication is not supported by this realm: $id")

                    provider.login(realm = this, email = request.email, password = request.password)
                }
            }

            val response = LoginResponse(
                token = generateJwt(user),
                user = serializeUser(user),
            )

            return response
        }
    }

    init {
        val duplicatedRealms = realms.groupBy { it.id }
            .filter { (id, realms) -> realms.size > 1 }
            .map { (id, realms) -> id }

        if (duplicatedRealms.isNotEmpty()) {
            throw error("Found duplicated authentication realms: $duplicatedRealms")
        }
    }

    suspend fun login(realm: String, request: LoginRequest): LoginResponse {

        val r = realms.firstOrNull { it.id == realm }
            ?: throw AuthError("Realm not found: $realm")

        return r.login(request)
    }
}
