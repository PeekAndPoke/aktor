package de.peekandpoke.aktor.frontend.layout

import de.peekandpoke.aktor.frontend.MainRouter
import de.peekandpoke.aktor.frontend.Nav
import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.css.marginLeft
import kotlinx.css.marginRight
import kotlinx.css.px
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.LoggedInLayout(
    inner: FlowContent.() -> Unit,
) = comp(
    LoggedInLayout.Props(inner = inner)
) {
    LoggedInLayout(it)
}

class LoggedInLayout(ctx: Ctx<Props>) : Component<LoggedInLayout.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val inner: FlowContent.() -> Unit,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        val u = user ?: return

        ui.sidebar.vertical.visible.menu {

            ui.basic.segment {
                +u.name
            }

            noui.item A {
                onClick { MainRouter.navToUri(Nav.dashboard()) }
                +"Dashboard"
            }

            noui.item A {
                onClick {
                    MainRouter.navToUri(Nav.login())
                    State.auth.logout()
                }
                icon.sign_out_alternate()
                +"Logout"
            }
        }

        div {
            css {
                marginLeft = 250.px
                marginRight = 50.px
            }

            props.inner(this)
        }
    }
}
