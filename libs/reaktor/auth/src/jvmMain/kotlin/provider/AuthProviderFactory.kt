package io.peekandpoke.reaktor.auth.provider

import io.peekandpoke.reaktor.auth.AuthSystem

class AuthProviderFactory(
    val deps: Lazy<AuthSystem.Deps>,
)
