package de.peekandpoke.aktor.frontend

import de.peekandpoke.aktor.frontend.layout.LoggedInLayout
import de.peekandpoke.aktor.frontend.pages.ChatPage
import de.peekandpoke.aktor.frontend.pages.DashboardPage
import de.peekandpoke.aktor.frontend.pages.LoginPage
import de.peekandpoke.aktor.frontend.pages.NotFoundPage
import de.peekandpoke.kraft.addons.routing.*

object Nav {
    val login = Static("/login")

    val dashboard = Static("")
    val dashboardSlash = Static("/")
    val chat = Route1("/chat/{id}")
    fun chat(id: String) = chat.buildUri(id)
}

fun RouterBuilder.mountNav() {
    val isLoggedIn: RouterMiddleware = routerMiddleware {
        console.log("isLoggedIn", AuthState().loggedInUser)

        if (AuthState().loggedInUser == null) {
            AuthState.redirectAfterLoginUri = uri
            redirectTo(Nav.login())
        }
    }

    mount(Nav.login) { LoginPage() }

    using(isLoggedIn) {
        mount(Nav.dashboard) { LoggedInLayout { DashboardPage() } }
        mount(Nav.dashboardSlash) { LoggedInLayout { DashboardPage() } }
        mount(Nav.chat) { LoggedInLayout { ChatPage(it["id"]) } }
    }

    catchAll {
        NotFoundPage()
    }
}
