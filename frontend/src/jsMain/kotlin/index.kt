package de.peekandpoke.aktor.frontend

import de.peekandpoke.aktor.frontend.state.AppState
import de.peekandpoke.funktor.auth.authState
import de.peekandpoke.kraft.Kraft
import de.peekandpoke.kraft.addons.routing.Router
import de.peekandpoke.kraft.addons.routing.router
import de.peekandpoke.kraft.vdom.VDomEngine
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

val win = window.asDynamic()

// Initialize kraft and external dependencies like timezones //////////////////
val kraft: Kraft = Kraft.initialize()

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
    auth = authState<AppUserModel>(api = Apis.auth, router = { MainRouter }),
)

val MainRouter: Router = createRouter()

fun main() {
    val mountPoint = document.getElementById("spa") as HTMLElement

    PreactVDomEngine(mountPoint, vdomEngineOptions()) { FrontendAppComponent() }

    MainRouter.navigateToWindowUri()
}

private fun vdomEngineOptions(): VDomEngine.Options {
    return VDomEngine.Options.default
}

private fun createRouter() = router {
    mountNav()
}
