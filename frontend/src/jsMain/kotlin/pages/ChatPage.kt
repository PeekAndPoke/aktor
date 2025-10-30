package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Apis
import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.aktor.frontend.common.AiConversationView
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.ComponentRef
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.JoinedPageTitle
import de.peekandpoke.kraft.semanticui.forms.UiCheckboxField
import de.peekandpoke.kraft.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.semanticui.forms.UiTextAreaComponent
import de.peekandpoke.kraft.semanticui.forms.old.select.SelectField
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.*
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import io.ktor.client.plugins.sse.*
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationRequest
import io.peekandpoke.aktor.shared.model.SseMessages
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.css.*
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div
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
        if (canSend()) {
            launch {
                val message = input.trim()
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
            .catch { console.error("Error sending chat message", it) }
            .firstOrNull()

        console.log("Response, response")

        response?.let {
            updateConversation(response.conversation)
        }
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Chat") }

        div {
            css {
                marginTop = 2.em
                marginBottom = 2.em
            }

            conversation(this) {
                loading { ui.basic.loading.segment() }
                error { ui.segment { +"Error loading conversation" } }
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
    }

    private fun FlowContent.renderInputs() {
        ui.form Form {
            key = "message-input"

            onSubmit { evt ->
                evt.preventDefault()
                evt.stopPropagation()
            }

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
