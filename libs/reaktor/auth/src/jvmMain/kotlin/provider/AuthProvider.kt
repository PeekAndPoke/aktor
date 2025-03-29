package io.peekandpoke.reaktor.auth.provider

import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.reaktor.auth.AuthError
import io.peekandpoke.reaktor.auth.AuthSystem
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
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
    suspend fun <USER> login(realm: AuthSystem.Realm<USER>, request: LoginRequest): Stored<USER>?

    /**
     * As api model
     */
    fun asApiModel(): AuthProviderModel
}
