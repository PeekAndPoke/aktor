package de.peekandpoke.aktor.frontend

import io.peekandpoke.aktor.shared.appuser.model.AppUserModel
import io.peekandpoke.reaktor.auth.AuthState
import io.peekandpoke.reaktor.auth.authState

object State {
    val auth: AuthState<AppUserModel> = authState<AppUserModel>(api = Apis.auth)
}
