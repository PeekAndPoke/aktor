package de.peekandpoke.aktor.frontend.common

import de.peekandpoke.aktor.frontend.common.markdown.MarkdownView
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.SemanticIconFn
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel
import kotlinx.css.*
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.AiConversationMessageView(
    message: AiConversationModel.Message,
) = comp(
    AiConversationMessageView.Props(
        message = message,
    )
) {
    AiConversationMessageView(it)
}

class AiConversationMessageView(ctx: Ctx<Props>) : Component<AiConversationMessageView.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val message: AiConversationModel.Message,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        when (val message = props.message) {
            is AiConversationModel.Message.User -> message.render(this)
            is AiConversationModel.Message.System -> message.render(this)
            is AiConversationModel.Message.Assistant -> message.render(this)
            is AiConversationModel.Message.Tool -> message.render(this)
        }
    }

    private fun AiConversationModel.Message.System.render(flow: FlowContent) {
        with(flow) {
            renderLeftFloatedRow {
                ui.orange.segment {
                    renderLeftIcon { orange.robot }
                    MarkdownView(content)
                }
            }
        }
    }

    private fun AiConversationModel.Message.Assistant.render(flow: FlowContent) {
        with(flow) {
            content?.takeIf { it.isNotBlank() }?.let { content ->
                renderLeftFloatedRow {
                    ui.green.segment {
                        renderLeftIcon { green.robot }
                        MarkdownView(content)
                    }
                }
            }

            toolCalls?.takeIf { it.isNotEmpty() }?.forEach { toolCall ->
                renderLeftFloatedRow {
                    ui.violet.segment {
                        renderLeftIcon { violet.hammer }

                        details {
                            summary {
                                b { +"Tool call: '${toolCall.name}' (${toolCall.id})" }
                            }
                            renderPre(toolCall.args.print())
                        }
                    }
                }
            }
        }
    }

    private fun AiConversationModel.Message.Tool.render(flow: FlowContent) {
        with(flow) {
            renderLeftFloatedRow {
                ui.purple.segment {
                    renderLeftIcon { purple.hammer }
                    details {
                        summary {
                            b { +"Tool Response: '${toolCall.name}' (${toolCall.id})" }
                        }
                        renderPre(content)
                    }
                }
            }
        }
    }

    private fun AiConversationModel.Message.User.render(flow: FlowContent) {
        with(flow) {
            renderRightFloatedRow {
                ui.blue.segment {
                    renderRightIcon { blue.user }
                    MarkdownView(content)
                }
            }
        }
    }

    private fun FlowContent.renderLeftFloatedRow(content: DIV.() -> Unit) {
        ui.fourteen.wide.left.floated.column {
            content()
        }
    }

    private fun FlowContent.renderRightFloatedRow(content: DIV.() -> Unit) {
        ui.fourteen.wide.right.floated.column {
            content()
        }
    }

    private fun HtmlBlockTag.renderPre(content: String) {
        pre {
            css {
                maxHeight = 50.vh
                overflow = Overflow.auto
                backgroundColor = Color("#f5f5f5")
                padding = Padding(10.px)
                borderRadius = 5.px
                border = Border(1.px, BorderStyle.solid, Color("#ccc"))
            }
            +content
        }
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
