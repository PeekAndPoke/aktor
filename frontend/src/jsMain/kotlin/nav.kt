package de.peekandpoke.aktor.frontend

import de.peekandpoke.aktor.frontend.pages.ChatPage
import de.peekandpoke.aktor.frontend.pages.NotFoundPage
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static

object Nav {
    val chat = Static("/chat")
}

fun RouterBuilder.mountNav() {
    mount(Nav.chat) { ChatPage() }

    catchAll {
        NotFoundPage()
    }
}
