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
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import io.peekandpoke.aktor.shared.credentials.model.UserCredentialsModel
import kotlinx.browser.window
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.css.em
import kotlinx.css.marginBottom
import kotlinx.css.marginTop
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.CredentialsViewPage() = comp {
    CredentialsViewPage(it)
}

class CredentialsViewPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user!!

    private val loader = dataLoader {
        Apis.credentials.credentials
            .list(user = user.id)
            .map { it.data!! }
    }
    private val noDblClick = doubleClickProtection()

    private sealed interface States {
        data object View : States

        sealed interface GoogleFlow : States {
            data class Init(val scopes: List<String>) : GoogleFlow
            data class Running(val url: String) : GoogleFlow
        }
    }

    private var state: States by value(States.View)

    private suspend fun startGoogleFlow(scopes: List<String>) = noDblClick.runBlocking {
        state = States.GoogleFlow.Init(scopes)

        // Get the current host url
        val baseUrl = window.location.protocol + "//" + window.location.host
        val callbackUri = router.strategy.render(Nav.Credentials.googleCallback())
        val redirectUri = "$baseUrl$callbackUri"

        console.log("redirectUri", redirectUri)

        val result = Apis.credentials.credentials
            .initGoogleFlow(user = user.id, scopes = scopes, redirectUri = redirectUri)
            .catch { console.error("error while starting google flow: $it") }
            .firstOrNull()

        when (val d = result?.data) {
            null -> {
                toasts.error("Error while starting Google Flow")
                state = States.View
            }

            else -> {
                state = States.GoogleFlow.Running(url = d.url)
                // goto oauth flow
                window.location.href = d.url
            }
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Credentials") }

        div {
            css {
                marginTop = 2.em
                marginBottom = 2.em
            }

            ui.header H1 { +"Credentials" }
            +"No credentials yet"

            ui.divider()

            when (val s = state) {
                is States.View -> renderView()
                is States.GoogleFlow -> renderGoogleFlow(s)
            }
        }
    }

    private fun FlowContent.renderView() {

        ui.horizontal.list {
            noui.item {
                ui.button {
                    // TODO: add intermediate state to ask which permissions to grant
                    //   -> ask AI for the available scopes
                    onClick {
                        launch {
                            startGoogleFlow(
                                scopes = listOf(
                                    "https://www.googleapis.com/auth/calendar",
                                    "https://www.googleapis.com/auth/calendar.events",
                                )
                            )
                        }
                    }
                    +"Connect Google Calendar"
                }
            }
        }

        ui.divider()

        loader(this) {
            error { +"Error loading credentials" }
            loading { +"Loading credentials ..." }
            loaded { credentials ->
                if (credentials.isEmpty()) {
                    ui.message { +"No credentials yet" }
                } else {
                    ui.four.cards {
                        credentials.forEach { c ->
                            noui.card {
                                noui.content {
                                    noui.header {
                                        +(c::class.simpleName ?: "n/a")
                                    }
                                }
                                noui.content {
                                    when (c) {
                                        is UserCredentialsModel.GoogleOAuth2 -> {
                                            +c.scopes.joinToString(", ")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderGoogleFlow(s: States.GoogleFlow) {
        div {
            +"Connecting Google Calendar ..."
        }

        div {
            +s.toString()
        }
    }
}
