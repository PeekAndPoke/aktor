package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.AuthState
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

    private var errorMessage by value<String?>(null)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    private suspend fun login() = noDblClick.runBlocking {

        errorMessage = null

        val result = AuthState.loginWithPassword(user = user, password = password)

        if (result.isLoggedIn) {
            result.let {
                when (val uri = AuthState.redirectAfterLoginUri) {
                    null -> MainRouter.navToUri(Nav.dashboard())
                    else -> MainRouter.navToUri(uri)
                }
            }
        } else {
            errorMessage = "Login failed"
        }
    }

    override fun VDom.render() {

        ui.container {
            ui.header { +"Login" }

            ui.form Form {
                UiInputField(::user) {
                    label("User")
                }

                UiPasswordField(::password) {
                    label("Password")
                }

                ui.orange.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                    onClick {
                        launch { login() }
                    }
                    +"Login"
                }
            }

            errorMessage?.let { message ->
                ui.divider()
                ui.error.message { +message }
            }
        }
    }
}
