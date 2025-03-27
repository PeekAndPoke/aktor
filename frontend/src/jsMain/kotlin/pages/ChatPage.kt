package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Apis
import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.aktor.frontend.common.AiConversationView
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.addons.semanticui.forms.UiCheckboxField
import de.peekandpoke.kraft.addons.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.addons.semanticui.forms.UiTextAreaComponent
import de.peekandpoke.kraft.addons.semanticui.forms.old.select.SelectField
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.ktor.client.plugins.sse.*
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationRequest
import io.peekandpoke.aktor.shared.model.SseMessages
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.css.Position
import kotlinx.css.em
import kotlinx.css.position
import kotlinx.css.top
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

@Suppress("FunctionName")
fun Tag.ChatPage(id: String) = comp(
    ChatPage.Props(
        id = id
    )
) {
    ChatPage(it)
}

class ChatPage(ctx: Ctx<Props>) : Component<ChatPage.Props>(ctx) {

    data class Props(
        val id: String,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user!!

    private var input by value("")

    private val conversation = dataLoader {
        Apis.appUser.conversations
            .get(user = user.id, conversation = props.id)
            .map { it.data!! }
    }

    private val llms = dataLoader {
        Apis.llms.llms.list()
            .map { it.data!! }
            .onEach {
                if (selectedLlm == null) {
                    selectedLlm = it.firstOrNull()?.id
                }
            }
    }

    private var selectedLlm: String? by value(null)

    private var showToolCalls: Boolean by value(false)

    val textAreaRef = ComponentRef.Tracker<UiTextAreaComponent>()

    val noDblClick = doubleClickProtection()

    var sseSession: ClientSSESession? = null

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                console.log("mounting ChatPage ...")

                launch {
                    sseSession = Apis.appUser.sse.connect(user.id)

                    sseSession?.incoming?.collect { event ->
//                        console.log("SSE: received event: ${event.data}")

                        val payload = try {
                            Json.decodeFromString<SseMessages>(event.data ?: "")
                        } catch (_: Exception) {
                            return@collect
                        }

                        when (payload) {
                            is SseMessages.AiConversationUpdate -> {
                                updateConversation(payload.data)
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

    private fun updateConversation(new: AiConversationModel) {
        conversation.modifyValue { new }
        triggerRedraw()
    }

    fun canSend() = noDblClick.canRun && input.isNotBlank()

    private fun send() {
        val message = input.trim()

        if (canSend()) {
            launch {
                sendChat(message)
                input = ""
                delay(300.milliseconds)
                textAreaRef { it.focus() }
            }
        }
    }

    private suspend fun sendChat(message: String) = noDblClick.runBlocking {
        val response = Apis.appUser.conversations
            .send(
                user = user.id,
                conversation = props.id,
                message = AiConversationRequest.Send(
                    llmId = selectedLlm,
                    message = message,
                ),
            ).map { it.data!! }
            .firstOrNull()

        response?.let {
            updateConversation(response.conversation)
        }
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Chat") }

        conversation(this) {
            error { +"Error loading conversation" }
            loading { +"Loading ..." }
            loaded { data ->

                ui.grid {
                    ui.twelve.wide.column {
                        renderConversation(data)

                        ui.hidden.divider()

                        renderInputs()

                        ui.hidden.divider()
                    }

                    ui.four.wide.column {
                        renderSettings(data)
                    }
                }
            }
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

            ui.field {
                val canSend = canSend()
                ui.fluid.givenNot(noDblClick.canRun) { loading }.button {
                    if (canSend) {
                        onClick { send() }
                    }
                    +"Send"
                }
            }
        }
    }

    private fun FlowContent.renderConversation(conversation: AiConversationModel) {
        AiConversationView(
            conversation = conversation,
            options = AiConversationView.Options(
                showToolCalls = showToolCalls,
            ),
        )
    }

    private fun FlowContent.renderSettings(conversation: AiConversationModel) {

        ui.form {
            css {
                position = Position.sticky
                top = 2.em
            }

            ui.dividing.header { +"LLM" }

            llms(this) {
                loading { +"Loading ..." }
                error { +"Error loading LLMs" }
                loaded { data ->

                    SelectField(
                        value = selectedLlm,
                        onChange = { selectedLlm = it },
                    ) {
                        data.forEach { llm ->
                            option(llm.id, llm.id) {
                                +llm.description
                            }
                        }
                    }
                }
            }

            ui.dividing.header { +"Tools" }

            UiCheckboxField(::showToolCalls) {
                label("Show tool calls")
                toggle()
            }

            ui.list {
                conversation.tools.forEach { tool ->
                    noui.item {
                        noui.header { +tool.name }
                        noui.meta { +(tool.description.lines().firstOrNull { it.isNotBlank() } ?: "") }
                    }
                }
            }
        }
    }
}
