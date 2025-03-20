package de.peekandpoke.aktor.frontend

import de.peekandpoke.kraft.Kraft
import de.peekandpoke.kraft.addons.modal.ModalsManager
import de.peekandpoke.kraft.addons.popups.PopupsManager
import de.peekandpoke.kraft.addons.routing.Router
import de.peekandpoke.kraft.addons.routing.router
import de.peekandpoke.kraft.vdom.VDomEngine
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
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

val Modals: ModalsManager = ModalsManager()
val Popups: PopupsManager = PopupsManager()

val MainRouter: Router = createRouter()

// TODO: token provider
val Apis = WebAppApis(Config) { "" }

val Api = ChatClient()

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
