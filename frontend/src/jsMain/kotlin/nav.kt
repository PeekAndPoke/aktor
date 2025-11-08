package de.peekandpoke.aktor.frontend

import de.peekandpoke.aktor.frontend.layout.LoggedInLayout
import de.peekandpoke.aktor.frontend.pages.*
import de.peekandpoke.aktor.frontend.pages.credentials.CredentialsGoogleCallbackPage
import de.peekandpoke.aktor.frontend.pages.credentials.CredentialsViewPage
import de.peekandpoke.kraft.routing.Route1
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Static

object Nav {
    val login = Static("/login")

    val dashboard = Static("")
    val dashboardSlash = Static("/")

    val profile = Static("/profile")

    object Credentials {
        val view = Static("/credentials")
        val googleCallback = Static("/credentials/callback/google-oauth2")
    }

    val chat = Route1("/chat/{id}")
    fun chat(id: String) = chat.bind(id)
}

fun RouterBuilder.mountNav() {
    val authMiddleware = State.auth.routerMiddleWare(Nav.login())

    mount(Nav.login) { LoginPage() }

    middleware(authMiddleware) {
        layout({ LoggedInLayout { it() } }) {
            mount(Nav.dashboard) { DashboardPage() }
            mount(Nav.dashboardSlash) { DashboardPage() }

            mount(Nav.profile) { ProfilePage() }

            mount(Nav.chat) { ChatPage(it["id"]) }

            mount(Nav.Credentials.view) { CredentialsViewPage() }
            mount(Nav.Credentials.googleCallback) { CredentialsGoogleCallbackPage() }
        }
    }

    catchAll {
        NotFoundPage()
    }
}
