package io.peekandpoke.aktor.api.api

import de.peekandpoke.ktorfx.cluster.database
import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.aktor.api.AppUserApiFeature
import io.peekandpoke.aktor.api.SseSessions
import io.peekandpoke.aktor.backend.AiConversation
import io.peekandpoke.aktor.backend.AiConversationsRepo.Companion.asApiModel
import io.peekandpoke.aktor.backend.AppUser
import io.peekandpoke.aktor.backend.aiConversations
import io.peekandpoke.aktor.getBot
import io.peekandpoke.aktor.llm.ChatBot
import io.peekandpoke.aktor.shared.api.AppUserConversationsApiClient
import io.peekandpoke.aktor.shared.model.AiConversationRequest
import io.peekandpoke.aktor.shared.model.SseMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json

class AppUserConversationsApi(converter: OutgoingConverter) : ApiRoutes("conversations", converter) {

    data class GetConversationParam(
        override val user: Stored<AppUser>,
        val conversation: Stored<AiConversation>,
    ) : AppUserApiFeature.AppUserAware

    val list = AppUserConversationsApiClient.List.mount(AppUserApiFeature.SearchForAppUserParam::class) {
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

            ApiResponse.ok(
                Paged(
                    items = found.map { it.asApiModel() },
                    page = params.page,
                    epp = params.epp,
                    fullItemCount = found.fullCount
                )
            )
        }
    }

    val create = AppUserConversationsApiClient.Create.mount(AppUserApiFeature.AppUserParam::class) {
        docs {
            name = "Create"
        }.codeGen {
            funcName = "create"
        }.authorize {
            public()
        }.handle { params, body ->

            val new = AiConversation(
                ownerId = params.user._id,
                messages = listOf(
                    ChatBot.defaultSystemMessage
                )
            )

            val saved = database.aiConversations.insert(new)

            ApiResponse.ok(
                saved.asApiModel()
            )
        }
    }

    val get = AppUserConversationsApiClient.Get.mount(GetConversationParam::class) {
        docs {
            name = "get"
        }.codeGen {
            funcName = "get"
        }.authorize {
            public()
        }.handle { params ->

            ApiResponse.ok(
                params.conversation.asApiModel()
            )
        }
    }

    val send = AppUserConversationsApiClient.Send.mount(GetConversationParam::class) {
        docs {
            name = "send"
        }.codeGen {
            funcName = "send"
        }.authorize {
            public()
        }.handle { params, body ->

            val bot = call.application.async(SupervisorJob() + Dispatchers.IO) {
                call.getBot()
            }.await()

            var updated = params.conversation

            bot.chat(params.conversation.value, body.message).collect { update ->
                updated = updated.modify { update.conversation }

                SseSessions.session?.send(
                    event = "message",
                    data = Json.encodeToString<SseMessages>(
                        SseMessages.AiConversationUpdate(updated.asApiModel())
                    )
                )
            }

            // Save to the database
            val saved = database.aiConversations.save(updated)

            ApiResponse.ok(
                AiConversationRequest.Response(
                    conversation = saved.asApiModel(),
                )
            )
        }
    }
}
