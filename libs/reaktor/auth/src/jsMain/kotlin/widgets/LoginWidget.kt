package io.peekandpoke.reaktor.auth.widgets

import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.reaktor.auth.AuthState
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.AuthRealmModel
import io.peekandpoke.reaktor.auth.model.LoginRequest
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.serialization.json.jsonPrimitive

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

    private var email by value("")
    private var password by value("")

    private var errorMessage by value<String?>(null)

    val realmLoader = dataLoader {
        props.state.api.getRealm().map { it.data!! }
    }

    private val noDblClick = doubleClickProtection()

    private suspend fun login(request: LoginRequest) = noDblClick.runBlocking {

        errorMessage = null

        val result = props.state.login(request)

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

        errorMessage?.let {
            ui.error.message {
                +it
            }
        }

        fun dividerIfNotLast(idx: Int) {
            if (idx < realm.providers.size - 1) {
                ui.hidden.divider {}
            }
        }

        realm.providers.forEachIndexed { idx, provider ->
            val renderer: (FlowContent.() -> Unit)? = when (provider.type) {
                AuthProviderModel.TYPE_EMAIL_PASSWORD -> {
                    { renderEmailPasswordForm(provider) }
                }

                AuthProviderModel.TYPE_GOOGLE -> {
                    { renderGoogleSso(provider) }
                }

                else -> {
                    console.warn("LoginWidget: Unsupported login provider type: ${provider.type}")
                    null
                }
            }

            renderer?.let {
                it()
                dividerIfNotLast(idx)
            }
        }
    }

    private fun FlowContent.renderEmailPasswordForm(provider: AuthProviderModel) {
        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()
            }

            UiInputField(::email) {
                placeholder("User")
            }

            UiPasswordField(::password) {
                placeholder("Password")
            }

            ui.orange.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                onClick {
                    launch {
                        login(
                            LoginRequest.EmailAndPassword(provider = provider.id, email = email, password = password)
                        )
                    }
                }
                +"Login"
            }
        }
    }

    private fun FlowContent.renderGoogleSso(provider: AuthProviderModel) {
        val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

        console.log("Google client id", clientId)

        GoogleSignInButton(clientId = clientId) { token ->
            launch {
                login(
                    LoginRequest.OAuth(provider = provider.id, token = token)
                )
            }
        }
    }
}
