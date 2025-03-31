package de.peekandpoke.aktor.frontend

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.authState
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel

object State {
    val auth: AuthState<AppUserModel> = authState<AppUserModel>(
        api = Apis.auth,
        router = { MainRouter },
    )
}
