package io.peekandpoke.aktor.backend.aiconversation.api

import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.CalendarList
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.Events
import de.peekandpoke.funktor.cluster.database
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpTimezone
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
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
import io.peekandpoke.aktor.backend.credentials.credentials
import io.peekandpoke.aktor.backend.credentials.db.UserCredentials
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llms
import io.peekandpoke.aktor.llms.ChatBot
import io.peekandpoke.aktor.llms.Llm
import io.peekandpoke.aktor.mcp
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationRequest
import io.peekandpoke.aktor.shared.appuser.api.AppUserConversationsApiClient
import io.peekandpoke.aktor.shared.model.SseMessages
import kotlinx.serialization.json.*

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

            val result = Paged(
                items = found.map { it.asApiModel() },
                page = params.page,
                epp = params.epp,
                fullItemCount = found.fullCount
            )

            ApiResponse.ok(
                result
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
            // TODO: let the user choose the tools to be attached to the conversation
            val tools = getAllTools(params.user._id)

            val new = AiConversation(
                ownerId = params.user._id,
                messages = listOf(ChatBot.defaultSystemMessage),
                tools = tools.map { it.toToolRef() },
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

            val llm = llms.registry.getByIdOrDefault(body.llmId)
            val bot = ChatBot(llm = llm.llm, streaming = true)
            val tools = getAllTools(params.user._id)

            println("Sending message using llm ${llm.id}")

            var updated = params.conversation

            bot.chat(params.conversation.value, body.message, tools).collect { update ->
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

            ApiResponse.Companion.ok(
                AiConversationRequest.Response(
                    conversation = saved.asApiModel(),
                )
            )
        }
    }

    private suspend fun RoutingContext.getAllTools(userId: String): List<Llm.Tool> {
        val mcpTools = try {
            mcp?.listToolsBound() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val googleCalendarTools: List<Llm.Tool>? = credentials.googleOAuth?.let { googleOAuth ->
            val credentials = googleOAuth.getCredentialsWithAllScopes(
                userId = userId,
                scopes = listOf(
                    "https://www.googleapis.com/auth/calendar",
                    "https://www.googleapis.com/auth/calendar.events",
                )
            ) ?: return@let emptyList()

            GoogleCalendarTools(
                clientId = googleOAuth.clientId,
                clientSecret = googleOAuth.clientSecret,
                // TODO: get this from the user account
                timezone = MpTimezone.of("Europe/Berlin"),
                credentials = credentials,
            ).asLlmTools()
        }

        val tools = kontainer.get(ExampleBots::class).builtInTools
            .plus(mcpTools)
            .plus(googleCalendarTools ?: emptyList())

        return tools
    }
}

class GoogleCalendarTools(
    // TODO: create google calendar service
    private val clientId: String,
    // TODO: create google calendar service
    private val clientSecret: String,
    private val timezone: MpTimezone,
    private val credentials: Stored<UserCredentials.GoogleOAuth2>,
) {
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val dateFormat = "yyyy-MM-dd"
    private val dateTimeFormat = "yyyy-MM-dd HH:mm:ss"

    private val jsonCodec = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun asLlmTools(): List<Llm.Tool> {
        return listOf(
            Llm.Tool.Function(
                name = "google_calendar_list_calendars",
                description = "Lists all calendars",
                parameters = emptyList(),
                fn = ::listCalendarsFn
            ),
            Llm.Tool.Function(
                name = "google_calendar_list_events",
                description = "Lists events in a calendar",
                parameters = listOf(
                    Llm.Tool.StringParam(
                        name = "calendarId",
                        description = "The calendar ID",
                        required = true
                    ),
                    Llm.Tool.StringParam(
                        name = "start_date",
                        description = "The start date as YYYY-MM-DD",
                        required = true
                    ),
                    Llm.Tool.StringParam(
                        name = "end_date",
                        description = "The end date as YYYY-MM-DD (optional)",
                        required = false
                    )
                ),
                fn = ::listEventsFn
            )
        )
    }

    @Suppress("unused")
    private fun listCalendarsFn(args: AiConversation.Message.ToolCall.Args): String {
        val calendars = listCalendars()

        val data = buildJsonArray {
            calendars.items.forEach { cal ->
                addJsonObject {
                    put("id", cal.id)
                    put("summary", cal.summary)
                }
            }
        }

        return jsonCodec.encodeToString(data)
    }

    private fun listCalendars(): CalendarList {

        val calendarService = getCalendarService()
        val result = calendarService.calendarList().list().execute()

        return result
    }

    private fun listEventsFn(args: AiConversation.Message.ToolCall.Args): String {
        val calendarId = args.getString("calendarId")
            ?: error("Missing parameter 'calendarId'")

        val startDate = args.getString("start_date")?.let { MpLocalDate.tryParse(it) }
            ?: error("Missing parameter 'start_date'")

        val endDate = args.getString("end_date")?.let { MpLocalDate.tryParse(it) }
            ?: startDate.plusDays(30)

        val events = listEvents(calendarId = calendarId, from = startDate, to = endDate)

        val data = buildJsonArray {
            events.items.forEach {
                add(it.asJsonObject())
            }
        }

        return jsonCodec.encodeToString(data)
    }

    private fun listEvents(calendarId: String, from: MpLocalDate, to: MpLocalDate): Events {
        val calendarService = getCalendarService()
        val result = calendarService.events()
            .list(calendarId)
            .setTimeMin(DateTime(from.atStartOfDay(timezone).toEpochMillis()))
            .setTimeMax(DateTime(to.atStartOfDay(timezone).toEpochMillis()))
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()

        return result
    }

    private fun Event.asJsonObject(): JsonObject {
        val startDate = start.date?.value
            ?.let { MpZonedDateTime.fromEpochMillis(it, timezone).toLocalDate() }

        val startDateTime = start.dateTime?.takeIf { !it.isDateOnly }?.value
            ?.let { MpZonedDateTime.fromEpochMillis(it, timezone) }

        val endDate = end.date?.value
            ?.let { MpZonedDateTime.fromEpochMillis(it, timezone).toLocalDate() }

        val endDateTime = end.dateTime?.takeIf { !it.isDateOnly }?.value
            ?.let { MpZonedDateTime.fromEpochMillis(it, timezone) }

        val start = startDateTime?.format(dateTimeFormat) ?: startDate?.format(dateFormat)

        val startWeekday = (startDateTime?.toLocalDate() ?: startDate)?.dayOfWeek?.name?.lowercase()

        val end = endDateTime?.toIsoString() ?: endDate?.format(dateFormat)

        val endWeekday = (endDateTime?.toLocalDate() ?: endDate)?.dayOfWeek?.name?.lowercase()

        return buildJsonObject {
            put("id", id)
            put("summary", summary)
            put("description", description)
            put("status", status)
            put("start_date", start)
            put("start_weekday", startWeekday)
            put("end_date", end)
            put("end_weekday", endWeekday)
        }
    }

    private fun getCalendarService(): Calendar {
        val tokenServerUrl = GenericUrl("https://oauth2.googleapis.com/token")
        val clientAuth = ClientParametersAuthentication(clientId, clientSecret)

        val credential =
            Credential.Builder(com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod())
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setTokenServerUrl(tokenServerUrl)
                .setClientAuthentication(clientAuth)
                .build()
                .apply {
                    accessToken = credentials.value.accessToken
                    refreshToken = credentials.value.refreshToken
                }

        val calendarService = Calendar.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("Aktor")
            .build()

        return calendarService
    }
}
