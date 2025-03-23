package de.peekandpoke.aktor.frontend.common

import de.peekandpoke.aktor.frontend.common.AiConversationView.Options
import de.peekandpoke.kraft.addons.styling.StyleSheet
import de.peekandpoke.kraft.addons.styling.StyleSheets
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.AiConversationView(
    conversation: AiConversationModel,
    options: Options,
) = comp(
    AiConversationView.Props(
        conversation = conversation,
        options = options,
    )
) {
    AiConversationView(it)
}

class AiConversationView(ctx: Ctx<Props>) : Component<AiConversationView.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val conversation: AiConversationModel,
        val options: Options,
    )

    data class Options(
        val showToolCalls: Boolean,
    )

    private object Style : StyleSheet() {
        init {
            StyleSheets.mount(this)
        }

        val conversation = rule("conversation") {}
    }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val conversation get() = props.conversation
    private val options get() = props.options

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

            val adjusted = conversation.messages
                .mapNotNull { message ->
                    when (message) {
                        is AiConversationModel.Message.Assistant ->
                            message.copy(toolCalls = message.toolCalls?.takeIf { options.showToolCalls })

                        is AiConversationModel.Message.Tool ->
                            message.takeIf { options.showToolCalls }

                        else -> message
                    }
                }

            adjusted.forEach { message ->
                val isUser = message is AiConversationModel.Message.User

                noui.row {
                    id = message.uuid
                    key = message.uuid

                    if (isUser) {
                        ui.fourteen.wide.right.floated.column {
                            AiConversationMessageView(message, conversation, options)
                        }

                    } else {
                        ui.fourteen.wide.left.floated.column {
                            AiConversationMessageView(message, conversation, options)
                        }
                    }
                }
            }
        }
    }
}
