package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.funktor.auth.widgets.ChangePasswordWidget
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.ProfilePage() = comp {
    ProfilePage(it)
}

class ProfilePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user!!

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.container {

            ui.cards {
                noui.card {
                    noui.content {
                        ui.header { +"Pofile" }

                        ui.list {
                            noui.item {
                                noui.header { +"Name" }
                                noui.description { +user.name }
                            }
                            noui.item {
                                noui.header { +"Email" }
                                noui.description { +user.email }
                            }
                        }
                    }
                }

                noui.card {
                    noui.content {
                        ui.header { +"Change Password" }

                        ChangePasswordWidget(State.auth)
                    }
                }
            }
        }
    }
}
