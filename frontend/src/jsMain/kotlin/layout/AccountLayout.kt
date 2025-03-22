package de.peekandpoke.aktor.frontend.layout

import de.peekandpoke.aktor.frontend.MainRouter
import de.peekandpoke.aktor.frontend.Nav
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.css
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
fun Tag.AccountLayout(
    inner: FlowContent.() -> Unit,
) = comp(
    AccountLayout.Props(inner = inner)
) {
    AccountLayout(it)
}

class AccountLayout(ctx: Ctx<Props>) : Component<AccountLayout.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val inner: FlowContent.() -> Unit,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.sidebar.vertical.visible.menu {
            noui.item A {
                onClick { MainRouter.navToUri(Nav.dashboard()) }
                +"Dashboard"
            }
        }

        div {
            css {
                marginLeft = 250.px
                marginRight = 50.px
            }
            ui.container {
                props.inner(this)
            }
        }
    }
}
