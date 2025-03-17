package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Api
import de.peekandpoke.aktor.frontend.common.AiConversationView
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.addons.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.addons.semanticui.forms.UiTextAreaComponent
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.ktor.client.plugins.sse.*
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.SseMessages
import kotlinx.coroutines.cancel
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.serialization.json.Json

@Suppress("FunctionName")
fun Tag.ChatPage() = comp {
    ChatPage(it)
}

class ChatPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var input by value("")

    private var conversation by value(AiConversation.new)

    val textAreaRef = ComponentRef.Tracker<UiTextAreaComponent>()

    val noDblClick = doubleClickProtection()

    var sseSession: ClientSSESession? = null

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                console.log("mounting ChatPage ...")

                launch {
                    loadChat()
                }

                launch {
                    sseSession = Api.sse()

                    sseSession?.incoming?.collect { event ->
                        console.log("SSE: received event: ${event.data}")

                        val payload = try {
                            Json.decodeFromString<SseMessages>(event.data ?: "")
                        } catch (_: Exception) {
                            return@collect
                        }

                        when (payload) {
                            is SseMessages.AiConversationMessage -> {
                                conversation = payload.data
                            }
                        }
                    }
                }

                console.log("mounting ChatPage ... done!")
            }
            onUnmount {
                sseSession?.cancel()
                sseSession = null
            }
        }
    }

    private suspend fun loadChat() = noDblClick.runBlocking {
        val response = Api.loadChat()
        conversation = response
    }

    fun canSend() = noDblClick.canRun && input.isNotBlank()

    private fun send() {
        val message = input.trim()

        if (canSend()) {
            launch {
                sendChat(message)
                input = ""
                textAreaRef { it.focus() }
            }
        }
    }

    private suspend fun sendChat(message: String) = noDblClick.runBlocking {
        val response = Api.sendMessage(message)

        conversation = response
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Chat") }

        ui.container {
            ui.header H1 { +"Chat" }

            renderConversation()

            ui.hidden.divider()

            renderInputs()
        }
    }

    private fun FlowContent.renderInputs() {
        ui.form {
            key = "message-input"

            UiTextArea(value = input, onChange = { input = it }) {
                name("message")
                disabled(noDblClick.cannotRun)

                customize {
                    onKeyDown { event ->
                        val noModifierKeys = event.ctrlKey.not() &&
                                event.altKey.not() &&
                                event.metaKey.not() &&
                                event.shiftKey.not()

                        if (event.key == "Enter" && noModifierKeys) {
                            send()
                        }
                    }
                }
            }.track(textAreaRef)

            val canSend = canSend()

            ui.fluid.givenNot(noDblClick.canRun) { loading }.button {
                if (canSend) {
                    onClick {
                        send()
                    }
                }

                +"Send"
            }
        }
    }

    private fun FlowContent.renderConversation() {
        AiConversationView(conversation)
    }
}
