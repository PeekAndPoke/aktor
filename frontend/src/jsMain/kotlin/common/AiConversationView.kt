package de.peekandpoke.aktor.frontend.common

import de.peekandpoke.aktor.frontend.common.AiConversationView.Options
import de.peekandpoke.kraft.addons.styling.StyleSheet
import de.peekandpoke.kraft.addons.styling.StyleSheets
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel
import kotlinx.html.HTMLTag
import kotlinx.html.Tag
import kotlinx.html.style
import kotlinx.html.unsafe

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
                AiConversationMessageView(message)
            }
        }
    }
}
