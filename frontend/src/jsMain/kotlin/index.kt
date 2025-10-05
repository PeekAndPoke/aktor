package de.peekandpoke.aktor.frontend

import de.peekandpoke.aktor.frontend.state.AppState
import de.peekandpoke.funktor.auth.authState
import de.peekandpoke.kraft.kraftApp
import de.peekandpoke.kraft.routing.Router
import de.peekandpoke.kraft.semanticui.semanticUI
import de.peekandpoke.kraft.semanticui.toasts
import de.peekandpoke.kraft.semanticui.toasts.ToastsStage
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel
import kotlinx.browser.window

val win = window.asDynamic()

// Initialize config //////////////////////////////////////////////////////////

val Config = WebAppConfig().let {
    when (val tweak = win["tweakConfig"]) {
        null -> it
        else -> tweak(it) as WebAppConfig
    }
}.apply {
    console.log("Config", this)
}

val Apis: WebAppApis = WebAppApis(Config) { State.auth().token?.token }

val State: AppState = AppState(
    auth = authState<AppUserModel>(
        api = Apis.auth,
        router = { kraft.appAttributes[Router.key]!! }
    ),
)

// Initialize kraft and external dependencies like timezones //////////////////
val kraft = kraftApp {
    semanticUI {
        toasts {
            stageOptions = ToastsStage.Options(
                positioning = { top.right }
            )
        }
    }

    routing {
        usePathStrategy()

        mountNav()
    }
}

fun main() {
    kraft.mount(selector = "#spa", engine = PreactVDomEngine()) {
        FrontendAppComponent()
    }
}
