package de.peekandpoke.aktor.frontend

import de.peekandpoke.aktor.frontend.pages.ChatPage
import de.peekandpoke.aktor.frontend.pages.LoginPage
import de.peekandpoke.aktor.frontend.pages.NotFoundPage
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.RouterMiddleware
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.kraft.addons.routing.routerMiddleware

object Nav {
    val login = Static("/login")
    val chat = Static("/chat")
}

fun RouterBuilder.mountNav() {
    val isLoggedIn: RouterMiddleware = routerMiddleware {
//        if (state.auth().isNotLoggedIn || state.auth().user == null) {
//            state.redirectAfterLoginUri = uri
//            redirectTo(com.thebase.frontend.webapp.Nav.login())
//        }
    }

    mount(Nav.login) { LoginPage() }

    with(isLoggedIn) {
        mount(Nav.chat) { ChatPage() }
    }

    catchAll {
        NotFoundPage()
    }
}
