package de.peekandpoke.aktor.frontend.common

import de.peekandpoke.kraft.addons.styling.StyleSheet
import de.peekandpoke.kraft.addons.styling.StyleSheets
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.aktor.shared.model.AiConversationModel
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.AiConversationView(
    conversation: AiConversationModel,
) = comp(
    AiConversationView.Props(conversation = conversation)
) {
    AiConversationView(it)
}

class AiConversationView(ctx: Ctx<Props>) : Component<AiConversationView.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val conversation: AiConversationModel,
    )

    private object Style : StyleSheet() {
        init {
            StyleSheets.mount(this)
        }

        val conversation = rule("conversation") {}
    }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val conversation get() = props.conversation

    fun HTMLTag.unsafeCss(
        /** @Language("css") **/
        css: String,
    ) {
        unsafe {
            +css
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.grid.with(Style.conversation).then {

            style {
                unsafeCss(
                    """
                        .${Style.conversation} .markdown img { max-width: 100%; }
                        .${Style.conversation} .markdown ol li { margin-bottom: 1em; }
                    """.trimIndent()
                )
            }

            conversation.messages.forEach { message ->
                val isUser = message is AiConversationModel.Message.User

                noui.row {
                    id = message.uuid
                    key = message.uuid

                    if (isUser) {
                        ui.fourteen.wide.right.floated.column {
                            AiConversationMessageView(message)
                        }

                    } else {
                        ui.fourteen.wide.left.floated.column {
                            AiConversationMessageView(message)
                        }
                    }
                }
            }
        }
    }
}
