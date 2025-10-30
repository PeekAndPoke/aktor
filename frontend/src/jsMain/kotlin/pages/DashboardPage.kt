package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Apis
import de.peekandpoke.aktor.frontend.Nav
import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.Router.Companion.router
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMm
import de.peekandpoke.ultra.common.ellipsis
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.css.*
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.DashboardPage() = comp {
    DashboardPage(it)
}

class DashboardPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user!!

    private val noDblClick = doubleClickProtection()

    val chats = dataLoader {
        Apis.appUser.conversations
            .list(user = user.id)
            .map { it.data!! }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    private suspend fun createChat(): AiConversationModel? = noDblClick.runBlocking {
        val created = Apis.appUser.conversations
            .create(user = user.id)
            .map { it.data!! }
            .firstOrNull()

        created
    }

    override fun VDom.render() {
        ui.segment {
            ui.basic.blue.givenNot(noDblClick.canRun) { loading }.button {
                onClick { evt ->
                    launch {
                        createChat()?.let {
                            router.navToUri(evt, Nav.chat(id = it.id))
                        }
                    }
                }
                icon.comments()
                +"New Chat"
            }
        }

        ui.segment {
            chats(this) {
                loading { +"Loading ..." }
                error { +"No chats found" }
                loaded { data ->

                    ui.divided.very.relaxed.list {

                        data.items.forEach { chat ->
                            noui.item A {
                                onClick { evt ->
                                    router.navToUri(evt, Nav.chat(id = chat.id))
                                }

                                noui.header {

                                    ui.horizontal.list {
                                        noui.item {
                                            +chat.createdAt.atSystemDefaultZone().formatDdMmmYyyyHhMm()
                                        }
                                        noui.item {
                                            icon.comments()
                                            +"${chat.stats.numTotal}"
                                        }
                                        noui.item {
                                            icon.robot()
                                            +"${chat.stats.numAssistant}"
                                        }
                                        noui.item {
                                            icon.hammer()
                                            +"${chat.stats.numTool}"
                                        }
                                        noui.item {
                                            icon.user()
                                            +"${chat.stats.numUser}"
                                        }
                                    }
                                }

                                noui.description {
                                    css {
                                        whiteSpace = WhiteSpace.nowrap
                                        overflow = Overflow.hidden
                                        textOverflow = TextOverflow.ellipsis
                                    }
                                    val contents = chat.messages
                                        .filterIsInstance<AiConversationModel.Message.User>()
                                        .joinToString(separator = " ") { it.content }

                                    +contents.ellipsis(200)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
