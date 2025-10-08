package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.model.*
import de.peekandpoke.ultra.vault.Stored

interface AuthProvider {
    /**
     * Unique id of the provider with the realm
     */
    val id: String

    /**
     * Capabilities of the provider
     */
    val capabilities: Set<AuthProviderModel.Capability>

    /**
     * Tries to log in the user for the given [request].
     *
     * The user will be returned, when it is found and the request was validated successfully.
     *
     * Otherwise [AuthError] will be thrown.
     */
    suspend fun <USER> login(
        realm: AuthRealm<USER>,
        request: AuthLoginRequest,
    ): Stored<USER>

    /**
     * Updates specific things about the authentication setup of the user
     */
    suspend fun <USER> update(
        realm: AuthRealm<USER>,
        user: Stored<USER>,
        request: AuthUpdateRequest,
    ): AuthUpdateResponse {
        return AuthUpdateResponse(
            success = false,
        )
    }

    /**
     * Account recovery
     */
    suspend fun <USER> recover(
        realm: AuthRealm<USER>,
        request: AuthRecoveryRequest,
    ): AuthRecoveryResponse {
        return AuthRecoveryResponse(
            success = false,
        )
    }

    /**
     * As api model
     */
    fun asApiModel(): AuthProviderModel
}

/**
 * Checks if the provider has the given capability
 */
fun AuthProvider.hasCapability(capability: AuthProviderModel.Capability): Boolean = capability in capabilities

/**
 * Checks if the provider supports sign-in
 */
fun AuthProvider.supportsSignIn(): Boolean = hasCapability(AuthProviderModel.Capability.SignIn)

/**
 * Checks if the provider supports sign-up
 */
fun AuthProvider.supportsSignUp(): Boolean = hasCapability(AuthProviderModel.Capability.SignUp)

