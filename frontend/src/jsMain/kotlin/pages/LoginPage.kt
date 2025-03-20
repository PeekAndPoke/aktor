package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Apis
import de.peekandpoke.aktor.frontend.MainRouter
import de.peekandpoke.aktor.frontend.Nav
import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.aktor.shared.model.LoginWithPassword
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.LoginPage() = comp {
    LoginPage(it)
}

class LoginPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val noDblClick = doubleClickProtection()

    private var user by value("")
    private var password by value("")

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    private suspend fun login() = noDblClick.runBlocking {
        val result = Apis.login.withPassword(
            LoginWithPassword(user = user, password = password)
        ).firstOrNull()

        result.let {
            MainRouter.navToUri(Nav.chat())
        }
    }

    override fun VDom.render() {

        ui.container {
            ui.header { +"Login" }

            ui.form {
                UiInputField(::user) {
                    label("User")
                }

                UiPasswordField(::password) {
                    label("Password")
                }

                ui.orange.fluid.button {
                    onClick {
                        launch {
                            login()
                        }
                    }
                    +"Login"
                }
            }
        }

    }
}
