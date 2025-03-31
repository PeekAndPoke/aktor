package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthSystem
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthUpdateRequest
import de.peekandpoke.funktor.auth.model.AuthUpdateResponse
import de.peekandpoke.funktor.auth.model.LoginRequest
import de.peekandpoke.ultra.vault.Stored

interface AuthProvider {
    /**
     * Unique id of the provider with the realm
     */
    val id: String

    /**
     * Tries to log in the user for the given [request].
     *
     * The user will be returned, when it is found and the request was validated successfully.
     *
     * Otherwise [AuthError] will be thrown.
     */
    suspend fun <USER> login(
        realm: AuthSystem.Realm<USER>,
        request: LoginRequest,
    ): Stored<USER>?

    /**
     * Updates specific things about the authentication setup of the user
     */
    suspend fun <USER> update(
        realm: AuthSystem.Realm<USER>,
        user: Stored<USER>,
        request: AuthUpdateRequest,
    ): AuthUpdateResponse {
        return AuthUpdateResponse(
            success = false,
        )
    }

    /**
     * As api model
     */
    fun asApiModel(): AuthProviderModel
}
