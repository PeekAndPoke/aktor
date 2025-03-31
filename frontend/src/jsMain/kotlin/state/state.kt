package de.peekandpoke.aktor.frontend.state

import de.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel

class AppState(
    val auth: AuthState<AppUserModel>,
)
