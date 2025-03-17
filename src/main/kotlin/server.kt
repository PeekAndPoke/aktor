package io.peekandpoke.aktor

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.peekandpoke.aktor.chatbot.ChatBot
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.mcpclient.McpClient
import io.peekandpoke.aktor.model.SseMessages
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val server = EngineMain.createServer(args)

    server.start(wait = true)
}

val kontainer = blueprint.create()

@Suppress("unused")
fun Application.module() {

    var bot: ChatBot? = null
    var sseSession: ServerSSESession? = null

    val keys = kontainer.get(KeysConfig::class)
    val exampleBots = kontainer.get(ExampleBots::class)

    suspend fun getBot(): ChatBot {
        bot?.let { return it }

        val mcpClient = McpClient(
            name = "Play",
            version = "1.0.0",
            toolNamespace = "playground",
        ).connect()

        val mcpTools = mcpClient.listToolsBound() ?: emptyList()

        val created = exampleBots.createOpenAiBot(
            apiKey = keys.config.getString("keys.OPEN_AI_TOKEN"),
            model = "gpt-4o-mini",
            streaming = true,
            tools = mcpTools,
        )

//        val created = ExampleBot.createOllamaBot(
//            config = config,
//            model = OllamaModels.LLAMA_3_2_3B,
//            streaming = false,
//            tools = mcpTools,
//        )

        return created.also { bot = it }
    }

    install(SSE)

    // Install CORS feature
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.AcceptEncoding)
        allowHeader(HttpHeaders.AcceptLanguage)
        allowHeader(HttpHeaders.AccessControlRequestHeaders)
        allowHeader(HttpHeaders.AccessControlRequestMethod)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.CacheControl)
        allowHeader("Last-Event-ID")

        allowCredentials = true

        // Or to be more specific:
        allowHost("localhost:25867", schemes = listOf("http", "https"))
        allowHost("127.0.0.1:25867", schemes = listOf("http", "https"))
    }

    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    routing {
        get("/") {
            call.respondText("Hello, world!")
        }

        get("/ping") {
            call.respondText("pong")
        }

        sse("/sse") {
            sseSession = this
            sseSession.heartbeat()
            while (sseSession.isActive == true) {
                delay(1000)
            }
        }

        get("/chat") {
            var bot = getBot()

            call.respond(bot.conversation.value)
        }

        put("/chat/{message}") {
            val message = call.parameters["message"] ?: "Hello, world!"

            var bot = getBot()

            bot.chat(message).collect { update ->
                when (update) {
                    is Llm.Update.Response -> {
                        update.content?.let { print(it) }
                    }

                    is Llm.Update.Stop -> {
                        println()
                    }

                    is Llm.Update.Info -> {
                        println(update.message)
                    }
                }

                System.out.flush()

                sseSession?.send(
                    event = "message",
                    data = Json.encodeToString<SseMessages>(
                        SseMessages.AiConversationMessage(bot.conversation.value)
                    )
                )
            }

            call.respond(bot.conversation.value)
        }
    }
}
