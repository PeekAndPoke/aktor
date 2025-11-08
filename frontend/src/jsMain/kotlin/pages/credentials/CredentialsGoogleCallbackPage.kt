package de.peekandpoke.aktor.frontend.pages.credentials

import de.peekandpoke.aktor.frontend.Apis
import de.peekandpoke.aktor.frontend.Nav
import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.JoinedPageTitle
import de.peekandpoke.kraft.routing.Router.Companion.router
import de.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.html.Tag
import org.w3c.dom.url.URLSearchParams

@Suppress("FunctionName")
fun Tag.CredentialsGoogleCallbackPage() = comp {
    CredentialsGoogleCallbackPage(it)
}

class CredentialsGoogleCallbackPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user


    // TODO: validate params
    // TODO: get state from CredentialsGoogleCallback
    //    -> do we need this at all? Should be enough to store information needed to create Credential on the backend side

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                // Callback query params are like:
                //  state: qfs5Vc...
                //  code:  4/0Ab3...
                //  scope: https://www.googleapis.com/auth/calendar.events
                val params = URLSearchParams(window.location.search)

                val userId = user?.id
                    ?: return@onMount showErrorAndNavBack("Not logged in")
                val state = params.get("state")
                    ?: return@onMount showErrorAndNavBack("Missing callback parameter 'state'")
                val code = params.get("code")
                    ?: return@onMount showErrorAndNavBack("Missing callback parameter 'code'")

                launch {
                    callBackend(userId = userId, state = state, code = code)
                }
            }
        }
    }

    private suspend fun callBackend(userId: String, state: String, code: String) {
        val result = Apis.credentials.credentials
            .completeGoogleFlow(user = userId, state = state, code = code)
            .catch {
                showErrorAndNavBack(it.message ?: "Unknown error")
            }
            .firstOrNull()

        when (result?.data) {
            null -> showErrorAndNavBack("Unknown error")
            else -> {
                toasts.info("Google OAuth flow completed successfully")
                router.navToUri(evt = null, route = Nav.Credentials.view())
            }
        }
    }

    private fun showErrorAndNavBack(error: String) {
        toasts.error("Google OAuth flow failed: $error")
        router.navToUri(evt = null, route = Nav.Credentials.view())
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Credentials") }

        ui.header H1 { +"Processing Google Callback" }

        ui.loader.segment { +"..." }
    }
}
