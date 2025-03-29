package io.peekandpoke.reaktor.auth.provider

import io.peekandpoke.reaktor.auth.model.AuthProviderModel

interface AuthProvider {
    fun asApiModel(): AuthProviderModel
}
