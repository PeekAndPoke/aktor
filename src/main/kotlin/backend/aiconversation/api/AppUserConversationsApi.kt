package io.peekandpoke.aktor.backend.aiconversation.api

import de.peekandpoke.funktor.cluster.database
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.vault.Stored
import io.ktor.server.routing.*
import io.peekandpoke.aktor.api.SseSessions
import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.backend.aiconversation.AiConversationsRepo.Companion.asApiModel
import io.peekandpoke.aktor.backend.aiconversation.aiConversations
import io.peekandpoke.aktor.backend.appuser.AppUser
import io.peekandpoke.aktor.backend.appuser.api.AppUserApiFeature
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.ChatBot
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llms
import io.peekandpoke.aktor.mcp
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationRequest
import io.peekandpoke.aktor.shared.appuser.api.AppUserConversationsApiClient
import io.peekandpoke.aktor.shared.model.SseMessages
import kotlinx.serialization.json.Json

class AppUserConversationsApi(converter: OutgoingConverter) : ApiRoutes("conversations", converter) {

    data class GetConversationParam(
        override val user: Stored<AppUser>,
        val conversation: Stored<AiConversation>,
    ) : AppUserApiFeature.AppUserAware

    val list = AppUserConversationsApiClient.Companion.List.mount(AppUserApiFeature.SearchForAppUserParam::class) {
        docs {
            name = "List"
        }.codeGen {
            funcName = "list"
        }.authorize {
            public()
        }.handle { params ->

            val found = database.aiConversations.findByOwner(
                ownerId = params.user._id,
                page = params.page,
                epp = params.epp,
            )

            ApiResponse.Companion.ok(
                Paged(
                    items = found.map { it.asApiModel() },
                    page = params.page,
                    epp = params.epp,
                    fullItemCount = found.fullCount
                )
            )
        }
    }

    val create = AppUserConversationsApiClient.Companion.Create.mount(AppUserApiFeature.AppUserParam::class) {
        docs {
            name = "Create"
        }.codeGen {
            funcName = "create"
        }.authorize {
            public()
        }.handle { params, body ->
            // TODO: let the user choose the tools to be attached to the conversation
            val tools = getAllTools()

            val new = AiConversation(
                ownerId = params.user._id,
                messages = listOf(ChatBot.defaultSystemMessage),
                tools = tools.map { it.toToolRef() },
            )

            val saved = database.aiConversations.insert(new)

            ApiResponse.Companion.ok(
                saved.asApiModel()
            )
        }
    }

    val get = AppUserConversationsApiClient.Companion.Get.mount(GetConversationParam::class) {
        docs {
            name = "get"
        }.codeGen {
            funcName = "get"
        }.authorize {
            public()
        }.handle { params ->

            ApiResponse.Companion.ok(
                params.conversation.asApiModel()
            )
        }
    }

    val send = AppUserConversationsApiClient.Companion.Send.mount(GetConversationParam::class) {
        docs {
            name = "send"
        }.codeGen {
            funcName = "send"
        }.authorize {
            public()
        }.handle { params, body ->

            val llm = llms.registry.getByIdOrDefault(body.llmId)
            val bot = ChatBot(llm = llm.llm, streaming = true)
            val tools = getAllTools()

            println("Sending message using llm ${llm.id}")

            var updated = params.conversation

            bot.chat(params.conversation.value, body.message, tools).collect { update ->
                updated = updated.modify { update.conversation }

                SseSessions.session?.send(
                    event = "message",
                    data = Json.Default.encodeToString<SseMessages>(
                        SseMessages.AiConversationUpdate(updated.asApiModel())
                    )
                )
            }

            // Save to the database
            val saved = database.aiConversations.save(updated)

            ApiResponse.Companion.ok(
                AiConversationRequest.Response(
                    conversation = saved.asApiModel(),
                )
            )
        }
    }

    private suspend fun RoutingContext.getAllTools(): List<Llm.Tool> {
        val mcpTools = try {
            mcp?.listToolsBound() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val tools = kontainer.get(ExampleBots::class).builtInTools
            .plus(mcpTools)

        return tools
    }
}
