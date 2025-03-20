package io.peekandpoke.aktor.shared.model

object AppUserRoles {
    const val Default = "appuser:default"

    /**
     * All roles
     */
    val all: Set<String> = setOf(
        Default,
    )
}
