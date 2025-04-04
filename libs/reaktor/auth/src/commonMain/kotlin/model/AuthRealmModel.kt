package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRealmModel(
    val id: String,
    val providers: List<AuthProviderModel>,
    val passwordPolicy: PasswordPolicy,
)
