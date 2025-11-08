package io.peekandpoke.aktor.backend.credentials

import io.peekandpoke.aktor.backend.credentials.db.UserCredentialsRepo
import io.peekandpoke.aktor.backend.credentials.services.GoogleOAuthService

class CredentialsServices(
    userCredentialsRepo: Lazy<UserCredentialsRepo>,
    googleOAuth: Lazy<GoogleOAuthService?>,
) {
    val userCredentialsRepo: UserCredentialsRepo by userCredentialsRepo
    val googleOAuth: GoogleOAuthService? by googleOAuth
}
