package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Api
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.addons.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.aktor.model.AiConversation
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.pre

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
                )

                ui.fluid.givenNot(noDblClick.canRun) { loading }.button {
                    onClick {
                        val message = input.trim()

                        launch {
                            sendChat(message)
                        }

                        input = ""
                    }
                    +"Send"
                }
            }
        }
    }

    private fun FlowContent.renderConversation() {
        ui.divided.list {
            conversation.messages.forEach { message ->
                noui.item {
                    when (message) {
                        is AiConversation.Message.System -> {
                            ui.label { +"System" }
                            ui.segment {
                                +message.content
                            }
                        }

                        is AiConversation.Message.Assistant -> {
                            ui.label { +"Assistant" }
                            ui.segment {
                                message.content?.let { content ->
                                    +content
                                }

                                message.toolCalls?.let { toolCalls ->
                                    if (toolCalls.isNotEmpty()) {
                                        ui.divider()
                                        ui.header H4 { +"Tool Calls:" }

                                        toolCalls.forEach { toolCall ->
                                            ui.segment {
                                                ui.header H5 { +"${toolCall.name} (ID: ${toolCall.id})" }
                                                ui.label { +"Arguments:" }
                                                pre {
                                                    +toolCall.args.print()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        is AiConversation.Message.User -> {
                            ui.blue.label { +"You" }
                            ui.blue.segment {
                                +message.content
                            }
                        }

                        is AiConversation.Message.Tool -> {
                            ui.grey.label { +"Tool Response: ${message.toolCall.name}" }
                            ui.grey.segment {
                                +message.content
                            }
                        }
                    }
                }
            }
        }
    }
}
