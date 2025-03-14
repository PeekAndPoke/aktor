package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Api
import de.peekandpoke.kraft.addons.marked.marked
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.addons.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.*
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.aktor.model.AiConversation
import kotlinx.css.*
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.ChatPage() = comp {
    ChatPage(it)
}

class ChatPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var input by value("")

    private var conversation by value(AiConversation.new)

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

    private suspend fun sendChat(message: String) = noDblClick.runBlocking {
        val response = Api.sendMessage(message)

        conversation = response
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Chat") }

        ui.container {
            ui.header H1 { +"Chat" }

            ui.divider()

            renderConversation()

            ui.divider()

            ui.form {

                UiTextArea(
                    value = input,
                    onChange = { input = it }
                ) {
                    name("message")
                    disabled(noDblClick.cannotRun)
                }

                ui.fluid.givenNot(noDblClick.canRun) { loading }.button {
                    onClick {
                        val message = input.trim()

                        launch {
                            sendChat(message)
                            input = ""
                        }
                    }

                    +"Send"
                }
            }
        }
    }

    private fun FlowContent.renderConversation() {
        ui.grid {
            conversation.messages.forEach { message ->
                noui.row {
                    when (message) {
                        is AiConversation.Message.System -> {
                            ui.twelve.wide.left.floated.column {
                                ui.orange.segment {
                                    renderLeftIcon { orange.robot }
                                    noui.content {
                                        renderMarkdown(message.content)
                                    }
                                }
                            }
                        }

                        is AiConversation.Message.Assistant -> {

                            ui.twelve.wide.left.floated.column {
                                message.content?.takeIf { it.isNotBlank() }?.let { content ->
                                    ui.green.segment {
                                        renderLeftIcon { green.comment }
                                        noui.content {
                                            renderMarkdown(content)
                                        }
                                    }
                                }

                                message.toolCalls?.takeIf { it.isNotEmpty() }?.forEach { toolCall ->
                                    ui.violet.segment {
                                        renderLeftIcon { violet.bolt }
                                        b { +"Tool call: '${toolCall.name}' (${toolCall.id})" }
                                        noui.content {
                                            pre {
                                                +toolCall.args.print()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        is AiConversation.Message.Tool -> {
                            ui.twelve.wide.left.floated.column {
                                ui.violet.segment {
                                    renderLeftIcon { violet.bolt }
                                    b { +"Tool Response: '${message.toolCall.name}' (${message.toolCall.id})" }
                                    pre {
                                        +message.content
                                    }
                                }
                            }
                        }

                        is AiConversation.Message.User -> {
                            ui.twelve.wide.right.floated.column {
                                ui.blue.segment {
                                    renderRightIcon { blue.user }
                                    noui.content {
                                        renderMarkdown(message.content)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DIV.renderMarkdown(content: String) {
        val md = marked.parse(content)

        unsafe { +md }
    }

    private fun DIV.renderLeftIcon(iconFn: SemanticIconFn) {

        icon.circular.inverted.iconFn().then {
            css {
                position = Position.absolute
                top = (-12).px
                left = (-12).px
            }
        }
    }

    private fun DIV.renderRightIcon(iconFn: SemanticIconFn) {

        icon.circular.inverted.iconFn().then {
            css {
                position = Position.absolute
                top = (-12).px
                right = (-12).px
            }
        }
    }
}
