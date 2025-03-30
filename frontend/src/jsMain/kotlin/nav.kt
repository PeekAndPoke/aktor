package de.peekandpoke.aktor.frontend

import de.peekandpoke.aktor.frontend.layout.LoggedInLayout
import de.peekandpoke.aktor.frontend.pages.*
import de.peekandpoke.kraft.addons.routing.Route1
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static

object Nav {
    val login = Static("/login")

    val dashboard = Static("")
    val dashboardSlash = Static("/")

    val profile = Static("/profile")

    val chat = Route1("/chat/{id}")
    fun chat(id: String) = chat.buildUri(id)
}

fun RouterBuilder.mountNav() {
    val authMiddleware = State.auth.routerMiddleWare(Nav.login())

    mount(Nav.login) { LoginPage() }

    using(authMiddleware) {
        mount(Nav.dashboard) { LoggedInLayout { DashboardPage() } }
        mount(Nav.dashboardSlash) { LoggedInLayout { DashboardPage() } }
        mount(Nav.profile) { LoggedInLayout { ProfilePage() } }
        mount(Nav.chat) { LoggedInLayout { ChatPage(it["id"]) } }
    }

    catchAll {
        NotFoundPage()
    }
}
