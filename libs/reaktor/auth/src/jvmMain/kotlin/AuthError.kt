package io.peekandpoke.reaktor.auth

open class AuthError(message: String, cause: Throwable? = null) : Throwable(message = message, cause = cause)
