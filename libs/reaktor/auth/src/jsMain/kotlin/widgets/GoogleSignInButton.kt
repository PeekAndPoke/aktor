package de.peekandpoke.funktor.auth.widgets

import com.benasher44.uuid.uuid4
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.data
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.GoogleSignInButton(
    clientId: String,
    onToken: (token: String) -> Unit,
) = comp(
    GoogleSignInButton.Props(clientId = clientId, onToken = onToken)
) {
    GoogleSignInButton(it)
}

class GoogleSignInButton(ctx: Ctx<Props>) : Component<GoogleSignInButton.Props>(ctx) {

    // See: https://developers.google.com/identity/gsi/web/guides/display-button?hl=de

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val clientId: String,
        val onToken: (token: String) -> Unit,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val cbName = "_google_sso_${uuid4().toString().replace("-", "")}"

    private var mounted by value(false)

    private fun handleGoogleSsoResponse(googleResponse: dynamic) {
        props.onToken(googleResponse.credential as String)
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                window.asDynamic()[cbName] = ::handleGoogleSsoResponse

                mounted = true
            }

            onUnmount {
                window.asDynamic()[cbName] = undefined
            }
        }
    }

    override fun VDom.render() {
        div {
            if (mounted) {
                script {
                    src = "https://accounts.google.com/gsi/client"
                    async = true
                }

                noui {
                    id = "g_id_onload"
                    data("client_id", props.clientId)
                    data("callback", cbName)
                    data("auto_prompt", "false")
                    data("auto_select", "false")
                }

                noui {
                    id = "g_id_signin"
                    classes = setOf("g_id_signin")
                    data("type", "standard")
                    data("theme", "outline")
                    data("text", "sign_in_with")
                    data("shape", "rectangular")
                    data("width", dom?.offsetWidth?.toString() ?: "")
                }
            }
        }
    }
}
