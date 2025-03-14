package de.peekandpoke.aktor.frontend

import de.peekandpoke.kraft.Kraft
import de.peekandpoke.kraft.addons.modal.ModalsManager
import de.peekandpoke.kraft.addons.popups.PopupsManager
import de.peekandpoke.kraft.addons.routing.Router
import de.peekandpoke.kraft.addons.routing.router
import de.peekandpoke.kraft.vdom.VDomEngine
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

// Initialize kraft and external dependencies like timezones //////////////////
val kraft: Kraft = Kraft.initialize()

val Modals: ModalsManager = ModalsManager()
val Popups: PopupsManager = PopupsManager()

val MainRouter: Router = createRouter()

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
