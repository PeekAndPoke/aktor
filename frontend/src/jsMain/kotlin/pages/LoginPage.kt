package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Nav
import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.reaktor.auth.widgets.LoginWidget
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.LoginPage() = comp {
    LoginPage(it)
}

class LoginPage(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {

        ui.container {
            ui.header { +"Login" }

            LoginWidget(
                state = State.auth,
                onLoginSuccessUri = Nav.dashboard(),
            )
        }
    }
}
