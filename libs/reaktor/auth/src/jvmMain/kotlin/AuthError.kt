package io.peekandpoke.reaktor.auth

open class AuthError(message: String, cause: Throwable? = null) : Throwable(message = message, cause = cause) {

    companion object {
        fun invalidCredentials(cause: Throwable? = null) = AuthError("Invalid credentials", cause)

        fun weekPassword(cause: Throwable? = null) = AuthError("Weak password", cause)
    }
}
