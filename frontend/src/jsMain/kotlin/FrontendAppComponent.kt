package de.peekandpoke.aktor.frontend

import de.peekandpoke.kraft.addons.routing.RouterComponent
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.FrontendAppComponent() = comp {
    FrontendAppComponent(it)
}

class FrontendAppComponent(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        console.log("rendering app ...")

        kraft.mount(this) {
            div(classes = "app") {
                RouterComponent(router = MainRouter)
            }
        }
    }
}
