package de.peekandpoke.funktor.auth.widgets

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.GithubSignInButton(
    clientId: String,
    callbackUrl: String,
    onToken: (token: String) -> Unit,
) = comp(
    GithubSignInButton.Props(
        clientId = clientId,
        callbackUrl = callbackUrl,
        onToken = onToken,
    )
) {
    GithubSignInButton(it)
}

class GithubSignInButton(ctx: Ctx<Props>) : Component<GithubSignInButton.Props>(ctx) {

    // https://medium.com/@rbscoop2611/how-to-add-login-with-github-functionality-in-your-react-project-using-oauth2-781e84247328

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val clientId: String,
        val callbackUrl: String,
        val onToken: (token: String) -> Unit,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////


    override fun VDom.render() {
        div {
            ui.fluid.basic.button {
                onClick {
                    val id = props.clientId
                    val cbUrl = props.callbackUrl
                    val url = "https://github.com/login/oauth/authorize?client_id=$id&redirect_uri=$cbUrl"

                    // Forward to Github
                    window.location.href = url
                }

                icon.github()
                +"Sign in with GitHub"
            }
        }
    }
}
