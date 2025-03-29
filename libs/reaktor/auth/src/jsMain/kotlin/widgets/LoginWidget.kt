package io.peekandpoke.reaktor.auth.widgets

import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.reaktor.auth.AuthState
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.AuthRealmModel
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun <USER> Tag.LoginWidget(
    state: AuthState<USER>,
    onLoginSuccessUri: String,
) = comp(
    LoginWidget.Props(
        state = state,
        onLoginSuccessUri = onLoginSuccessUri,
    )
) {
    LoginWidget(it)
}

class LoginWidget<USER>(ctx: Ctx<Props<USER>>) : Component<LoginWidget.Props<USER>>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props<USER>(
        val state: AuthState<USER>,
        val onLoginSuccessUri: String,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var user by value("")
    private var password by value("")

    private var errorMessage by value<String?>(null)

    val realmLoader = dataLoader {
        props.state.api.getRealm().map { it.data!! }
    }

    private val noDblClick = doubleClickProtection()

    private suspend fun loginWithEmailAndPassword() = noDblClick.runBlocking {

        errorMessage = null

        val result = props.state.loginWithPassword(user = user, password = password)

        console.log("Login result:", result)

        if (result.isLoggedIn) {
            props.state.redirectAfterLogin(
                props.onLoginSuccessUri
            )
        } else {
            errorMessage = "Login failed"
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        realmLoader(this) {
            loading {
                ui.basic.loading.segment {
                    icon.loading.spinner()
                }
            }

            error {
                ui.basic.segment {
                    onClick { realmLoader.reload() }
                    +"Login not possible. Please try again later."
                }
            }

            loaded { realm ->
                renderContent(realm)
            }
        }
    }

    private fun FlowContent.renderContent(realm: AuthRealmModel) {

        realm.providers.forEach { provider ->
            when (provider.type) {
                AuthProviderModel.TYPE_EMAIL_PASSWORD -> {
                    renderEmailPasswordForm()
                }

                else -> {
                    console.warn("LoginWidget: Unsupported login provider type: ${provider.type}")
                }
            }
        }
    }

    private fun FlowContent.renderEmailPasswordForm() {
        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()
            }

            UiInputField(::user) {
                label("User")
            }

            UiPasswordField(::password) {
                label("Password")
            }

            ui.orange.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                onClick {
                    launch { loginWithEmailAndPassword() }
                }
                +"Login"
            }
        }
    }
}
