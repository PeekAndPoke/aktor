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
import io.peekandpoke.aktor.model.AiConversation
import kotlinx.html.FlowContent
import kotlinx.html.Tag

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

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        launch {
            loadChat()
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
