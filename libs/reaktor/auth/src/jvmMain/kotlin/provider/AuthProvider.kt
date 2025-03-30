package io.peekandpoke.reaktor.auth.provider

import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.AuthError
import io.peekandpoke.reaktor.auth.AuthSystem
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.AuthUpdateRequest
import io.peekandpoke.reaktor.auth.model.AuthUpdateResponse
import io.peekandpoke.reaktor.auth.model.LoginRequest

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
