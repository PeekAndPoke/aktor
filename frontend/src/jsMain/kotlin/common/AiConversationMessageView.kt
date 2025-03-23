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
import io.peekandpoke.aktor.shared.model.AiConversationModel
import kotlinx.css.*
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.AiConversationMessageView(
    message: AiConversationModel.Message,
    conversation: AiConversationModel,
) = comp(
    AiConversationMessageView.Props(
        message = message,
        conversation = conversation,
    )
) {
    AiConversationMessageView(it)
}

class AiConversationMessageView(ctx: Ctx<Props>) : Component<AiConversationMessageView.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val message: AiConversationModel.Message,
        val conversation: AiConversationModel,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        when (val message = props.message) {
            is AiConversationModel.Message.System -> message.render(this)
            is AiConversationModel.Message.Assistant -> message.render(this)
            is AiConversationModel.Message.Tool -> message.render(this)
            is AiConversationModel.Message.User -> message.render(this)
        }
    }

    private fun AiConversationModel.Message.System.render(flow: FlowContent) {
        with(flow) {
            ui.orange.segment {
                renderLeftIcon { orange.robot }
                MarkdownView(content)
            }
        }
    }

    private fun AiConversationModel.Message.Assistant.render(flow: FlowContent) {
        with(flow) {
            content?.takeIf { it.isNotBlank() }?.let { content ->
                ui.green.segment {
                    renderLeftIcon { green.robot }
                    MarkdownView(content)
                }
            }

            toolCalls?.takeIf { it.isNotEmpty() }?.forEach { toolCall ->
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

    private fun AiConversationModel.Message.Tool.render(flow: FlowContent) {
        with(flow) {
            ui.violet.segment {
                renderLeftIcon { violet.hammer }

                details {
                    summary {
                        b { +"Tool Response: '${toolCall.name}' (${toolCall.id})" }
                    }

                    renderPre(content)
                }
            }
        }
    }

    private fun AiConversationModel.Message.User.render(flow: FlowContent) {
        with(flow) {
            ui.blue.segment {
                renderRightIcon { blue.user }
                MarkdownView(content)
            }
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
