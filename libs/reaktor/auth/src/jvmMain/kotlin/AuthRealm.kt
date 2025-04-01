package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.model.*
import de.peekandpoke.funktor.auth.provider.AuthProvider
import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.JsonObject

interface AuthRealm<USER> {

    interface Messaging<USER> {
        suspend fun sendPasswordChangedEmail(user: Stored<USER>): EmailResult

//        suspend fun sendPasswordResetEmil()
    }

    /** Unique id of the realm */
    val id: String

    /** Auth providers for this realm */
    val providers: List<AuthProvider>

    /** User messaging */
    val messaging: Messaging<USER>

    /** The password policy for this realm */
    val passwordPolicy: PasswordPolicy get() = PasswordPolicy.default

    suspend fun loadUserById(id: String): Stored<USER>?

    suspend fun loadUserByEmail(email: String): Stored<USER>?

    suspend fun generateJwt(user: Stored<USER>): AuthLoginResponse.Token

    suspend fun getUserEmail(user: Stored<USER>): String

    suspend fun serializeUser(user: Stored<USER>): JsonObject

    suspend fun login(request: AuthLoginRequest): AuthLoginResponse {
        val provider = providers.firstOrNull { it.id == request.provider }
            ?: throw AuthError("Provider not found: ${request.provider}")

        val user = provider.login<USER>(realm = this, request = request)
            ?: throw AuthError("User not found")

        val response = AuthLoginResponse(
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

    suspend fun recover(request: AuthRecoveryRequest): AuthRecoveryResponse {
        val provider = providers.firstOrNull { it.id == request.provider }
            ?: throw AuthError("Provider not found: ${request.provider}")

        val result = provider.recover(realm = this, request = request)

        return result
    }

    fun asApiModel(): AuthRealmModel = AuthRealmModel(
        id = id,
        providers = providers.map { it.asApiModel() },
        passwordPolicy = passwordPolicy,
    )
}
