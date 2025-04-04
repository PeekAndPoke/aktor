package de.peekandpoke.funktor.auth.widgets

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.model.*
import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.model.Message
import kotlinx.browser.window
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div
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

    private sealed interface DisplayState {
        data class Login(
            val email: String = "",
            val password: String = "",
            val message: Message? = null,
        ) : DisplayState

        data class RecoverPassword(
            val email: String = "",
            val provider: AuthProviderModel,
            val message: Message? = null,
        ) : DisplayState

        fun withMessage(message: Message?) = when (this) {
            is Login -> copy(message = message)
            is RecoverPassword -> copy(message = message)
        }
    }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var displayState: DisplayState by value(DisplayState.Login())

    val realmLoader = dataLoader {
        props.state.api.getRealm().map { it.data!! }
    }

    private val noDblClick = doubleClickProtection()

    init {
        realmLoader.value { realm ->
            realm?.let {
                // Handle any callback params ... f.e. from Github-OAuth
                val params = URLSearchParams(window.location.search)

                if (params.has(authCallbackParam)) {
                    val providerId = params.get(authCallbackParam)
                    val provider = realm.providers.find { it.id == providerId }

                    when (provider?.type) {
                        AuthProviderModel.TYPE_GITHUB -> {
                            params.get("code")?.let { code ->
                                login(
                                    AuthLoginRequest.OAuth(provider = provider.id, token = code)
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

    private fun login(request: AuthLoginRequest) {
        launch {
            doLogin(request)
        }
    }

    private suspend fun doLogin(request: AuthLoginRequest) = noDblClick.runBlocking {
        displayState = displayState.withMessage(message = null)

        val result = props.state.login(request)

        if (result.isLoggedIn) {
            props.state.redirectAfterLogin(props.onLoginSuccessUri)
        } else {
            displayState = displayState.withMessage(message = Message.error("Login failed"))
        }
    }

    private suspend fun recover(request: AuthRecoveryRequest): AuthRecoveryResponse? {
        return noDblClick.runBlocking {
            props.state.recover(request)
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
                when (val s = displayState) {
                    is DisplayState.Login -> renderLoginState(s, realm)
                    is DisplayState.RecoverPassword -> renderRecoverPasswordState(s)
                }
            }
        }
    }

    private fun FlowContent.renderMessage(message: Message?) = when (message?.type) {
        null -> Unit
        Message.Type.info -> ui.info.message { +message.text }
        Message.Type.warning -> ui.warning.message { +message.text }
        Message.Type.error -> ui.error.message { +message.text }
    }

    private fun FlowContent.renderLoginState(state: DisplayState.Login, realm: AuthRealmModel) {

        ui.header { +"Login" }

        renderMessage(state.message)

        fun dividerIfNotLast(idx: Int) {
            if (idx < realm.providers.size - 1) {
                ui.hidden.divider {}
            }
        }

        ui.list {
            realm.providers.forEachIndexed { idx, provider ->
                when (provider.type) {
                    AuthProviderModel.TYPE_EMAIL_PASSWORD -> noui.item {
                        renderEmailPasswordForm(state, provider)
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

    private fun FlowContent.renderEmailPasswordForm(state: DisplayState.Login, provider: AuthProviderModel) {
        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()
            }

            UiInputField(state.email, { displayState = state.copy(email = it) }) {
                placeholder("Email")
            }

            UiPasswordField(state.password, { displayState = state.copy(password = it) }) {
                placeholder("Password")
                revealPasswordIcon()
            }

            ui.field {
                ui.orange.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                    onClick {
                        login(
                            AuthLoginRequest.EmailAndPassword(
                                provider = provider.id,
                                email = state.email,
                                password = state.password,
                            )
                        )
                    }
                    +"Login"
                }
            }

            ui.field {
                a {
                    onClick { evt ->
                        evt.preventDefault()
                        displayState = DisplayState.RecoverPassword(
                            email = state.email,
                            provider = provider,
                        )
                    }
                    +"Forgot Password?"
                }
            }
        }
    }

    private fun FlowContent.renderGoogleSso(provider: AuthProviderModel) {
        val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

//        console.log("Google client id", clientId)

        GoogleSignInButton(clientId = clientId) { token ->
            login(
                AuthLoginRequest.OAuth(provider = provider.id, token = token)
            )
        }
    }

    private fun FlowContent.renderGithubSso(provider: AuthProviderModel) {
        val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

//        console.log("Google client id", clientId)

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
                AuthLoginRequest.OAuth(provider = provider.id, token = token)
            )
        }
    }

    private fun FlowContent.renderRecoverPasswordState(state: DisplayState.RecoverPassword) {

        div {
            onClick {
                displayState = DisplayState.Login(email = state.email)
            }
            icon.angle_left {}
            +"Back"
        }

        ui.hidden.divider {}

        renderMessage(state.message)

        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()
            }

            ui.header {
                +"Enter your email to recover your password"
            }

            UiInputField(state.email, { displayState = state.copy(email = it) }) {
                placeholder("Email")
            }

            ui.field {
                ui.orange.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                    onClick {
                        launch {
                            val result = recover(
                                AuthRecoveryRequest.ResetPassword(
                                    provider = state.provider.id,
                                    email = state.email,
                                )
                            )

                            displayState = when (result?.success) {
                                true -> DisplayState.Login(
                                    email = state.email,
                                    message = Message.info("Recovery email sent. Please check your inbox."),
                                )

                                else -> state.copy(message = Message.error("Recovery not possible"))
                            }
                        }
                    }
                    +"Recover Password"
                }
            }
        }
    }
}
