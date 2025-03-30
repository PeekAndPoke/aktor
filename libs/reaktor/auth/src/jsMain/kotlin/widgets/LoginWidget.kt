package io.peekandpoke.reaktor.auth.widgets

import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.reaktor.auth.AuthState
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.AuthRealmModel
import io.peekandpoke.reaktor.auth.model.LoginRequest
import kotlinx.browser.window
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.url.URLSearchParams

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

    companion object {
        const val authCallbackParam = "auth-callback"
    }

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

    init {
        realmLoader.value { realm ->
            realm?.let {
                val params = URLSearchParams(window.location.search)

                if (params.has(authCallbackParam)) {
                    val providerId = params.get(authCallbackParam)
                    val provider = realm.providers.find { it.id == providerId }

                    when (provider?.type) {
                        AuthProviderModel.TYPE_GITHUB -> {
                            params.get("code")?.let { code ->
                                login(
                                    LoginRequest.OAuth(provider = provider.id, token = code)
                                )
                            }
                        }
                    }

                    // Remove the excess query params from the uri
                    val parts = listOf(
                        window.location.origin,
                        window.location.pathname,
                        window.location.hash
                    )

                    window.history.pushState(null, "", parts.joinToString(""))
                }
            }
        }
    }

    private fun login(request: LoginRequest) = launch {
        doLogin(request)
    }

    private suspend fun doLogin(request: LoginRequest) = noDblClick.runBlocking {

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

        ui.list {
            realm.providers.forEachIndexed { idx, provider ->
                when (provider.type) {
                    AuthProviderModel.TYPE_EMAIL_PASSWORD -> noui.item {
                        renderEmailPasswordForm(provider)
                        dividerIfNotLast(idx)
                    }

                    AuthProviderModel.TYPE_GOOGLE -> noui.item {
                        renderGoogleSso(provider)
                    }

                    AuthProviderModel.TYPE_GITHUB -> noui.item {
                        renderGithubSso(provider)
                    }

                    else -> {
                        console.warn("LoginWidget: Unsupported login provider type: ${provider.type}")
                        null
                    }
                }
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
                revealPasswordIcon()
            }

            ui.orange.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                onClick {
                    login(
                        LoginRequest.EmailAndPassword(provider = provider.id, email = email, password = password)
                    )
                }
                +"Login"
            }
        }
    }

    private fun FlowContent.renderGoogleSso(provider: AuthProviderModel) {
        val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

        console.log("Google client id", clientId)

        GoogleSignInButton(clientId = clientId) { token ->
            login(
                LoginRequest.OAuth(provider = provider.id, token = token)
            )
        }
    }

    private fun FlowContent.renderGithubSso(provider: AuthProviderModel) {
        val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

        console.log("Google client id", clientId)

        val parts = listOf(
            window.location.origin,
            window.location.pathname,
            "?${authCallbackParam}=${provider.id}",
            window.location.hash,
        )

        val callbackUrl = parts.joinToString("")

        GithubSignInButton(
            clientId = clientId,
            callbackUrl = callbackUrl,
        ) { token ->
            login(
                LoginRequest.OAuth(provider = provider.id, token = token)
            )
        }
    }
}
