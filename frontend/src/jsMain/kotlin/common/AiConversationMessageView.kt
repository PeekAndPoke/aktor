package de.peekandpoke.aktor.frontend.common

import de.peekandpoke.aktor.frontend.common.markdown.renderMarkdown
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.SemanticIconFn
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.aktor.model.AiConversation
import kotlinx.css.*
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.AiConversationMessageView(
    message: AiConversation.Message,
) = comp(
    AiConversationMessageView.Props(message = message)
) {
    AiConversationMessageView(it)
}

class AiConversationMessageView(ctx: Ctx<Props>) : Component<AiConversationMessageView.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val message: AiConversation.Message,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        when (val message = props.message) {

            is AiConversation.Message.System -> {
                ui.fourteen.wide.left.floated.column {
                    ui.orange.segment {
                        renderLeftIcon { orange.robot }
                        renderMarkdown(message.content)
                    }
                }
            }

            is AiConversation.Message.Assistant -> {

                ui.fourteen.wide.left.floated.column {
                    message.content?.takeIf { it.isNotBlank() }?.let { content ->
                        ui.green.segment {
                            renderLeftIcon { green.comment }
                            renderMarkdown(content)
                        }
                    }

                    message.toolCalls?.takeIf { it.isNotEmpty() }?.forEach { toolCall ->
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

            is AiConversation.Message.Tool -> {
                ui.fourteen.wide.left.floated.column {
                    ui.violet.segment {
                        renderLeftIcon { violet.hammer }

                        details {
                            summary {
                                b { +"Tool Response: '${message.toolCall.name}' (${message.toolCall.id})" }
                            }

                            renderPre(message.content)
                        }
                    }
                }
            }

            is AiConversation.Message.User -> {
                ui.fourteen.wide.right.floated.column {
                    ui.blue.segment {
                        renderRightIcon { blue.user }
                        renderMarkdown(message.content)
                    }
                }
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
