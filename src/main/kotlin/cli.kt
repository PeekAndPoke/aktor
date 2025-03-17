package io.peekandpoke.aktor

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.mcpclient.CustomSSEClientTransport
import io.peekandpoke.aktor.mcpclient.McpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds


private suspend fun sseTest() {
    val jsonCodec = CustomSSEClientTransport.McpJson

    val sseClient = HttpClient {
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }
        install(SSE) {
        }
    }

    val postClient = HttpClient {
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }
    }

    val baseUrl = "http://localhost:8000"
//    val baseUrl = "http://localhost:8081"

    val sseSession = sseClient.sseSession(urlString = "$baseUrl/sse")

    fun Request.toJSON(): JSONRPCRequest {
        return JSONRPCRequest(
            method = method.value,
            params = jsonCodec.encodeToJsonElement(this),
            jsonrpc = JSONRPC_VERSION,
        )
    }

    val loop = coroutineScope {
        var msgEndpoint = ""

        suspend fun send(message: ClientRequest): HttpResponse {
            val url = "${baseUrl.trimEnd('/')}/${msgEndpoint.trimStart('/')}"

            val body = jsonCodec.encodeToString(message.toJSON())

            println("Sending message: $url \n $body")

            return postClient.post(url) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(body)
            }
        }

        suspend fun send(notification: ClientNotification): HttpResponse {
            val url = "${baseUrl.trimEnd('/')}/${msgEndpoint.trimStart('/')}"

            val body = JSONRPCNotification(
                method = notification.method.value,
                params = jsonCodec.encodeToJsonElement<Notification>(notification) as JsonObject,
            )

            println("Sending notification: $url \n $body")

            return postClient.post(url) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(jsonCodec.encodeToString(body))
            }
        }

        val scope = SupervisorJob() + Dispatchers.IO

        listOf(
            launch(scope) {
                sseSession.incoming
                    .onEach {
                        println("Received event: ${it.id} ${it.event}")
                        println(it.data)
                    }
                    .collect {
//                    println("Received event: ${it.event}")
//                    println("Received data: ${it.data}")

                        if (it.event == "endpoint") {
                            msgEndpoint = it.data!!

                            send(
                                InitializeRequest(
                                    protocolVersion = LATEST_PROTOCOL_VERSION,
                                    capabilities = ClientCapabilities(),
                                    clientInfo = Implementation(name = "test", version = "1.0.0")
                                )
                            )
                        }
                    }

                println(sseSession.call.response.status)

                error("Session was closed")
            },

            launch(scope) {
                delay(3.seconds)

                send(InitializedNotification())

                delay(3.seconds)

                repeat(100) {

                    println("Sending list tools")

                    send(ListToolsRequest())

                    delay(1.seconds)

                    println("Sending call tool")

                    send(
                        CallToolRequest(
                            name = "add",
                            arguments = jsonCodec.encodeToJsonElement(
                                mapOf("a" to 111, "b" to 222)
                            ) as JsonObject,
                        )
                    )

                    delay(10.seconds)
                }

//            send(
//                CallToolRequest(
//                    name = "add",
//                    arguments = jsonCodec.encodeToJsonElement(
//                        mapOf("a" to 111, "b" to 222)
//                    ) as JsonObject,
//                )
//            )
            }
        )
    }

    loop.joinAll()

    println("bye bye")

    exitProcess(0)
}


suspend fun main() {

//    val crawl4aiClient = Crawl4aiClient(
//        apiKey = "crawl4a-local-key",
//    )
//
//    val health = crawl4aiClient.healthCheck()
//
//    println("Health check: $health")
//
//    val result = crawl4aiClient
//        .crawlAsync(url="http://www.barleybrothers.de/")
//        .await()
//
//    println(result.resultAsObj)
//
//    exitProcess(0)

    //    sseTest()

    val kontainer = blueprint.create()

    val keys = kontainer.get(KeysConfig::class)
    val exampleBots = kontainer.get(ExampleBots::class)

    val mcpClient = McpClient(
        name = "Play",
        version = "1.0.0",
        toolNamespace = "playground",
    ).connect()

    val mcpTools = mcpClient.listToolsBound() ?: emptyList()

//    val bot = ExampleBot.createOllamaBot(
//        config = config,
//        model = OllamaModels.QWEN_2_5_14B,
//        streaming = false,
//    )
//
    val bot = exampleBots.createOpenAiBot(
        apiKey = keys.config.getString("keys.OPEN_AI_TOKEN"),
        model = "gpt-4o-mini",
        streaming = true,
        tools = mcpTools,
    )

    println("Chatting with model: ${bot.llm.model}")

    println("Available tools:")
    bot.llm.tools.forEach { tool ->
        println("- ${tool.describe().split("\n").first()}")

        when (tool) {
            is Llm.Tool.Function -> tool.parameters.forEach { p ->
                println("  param: ${p.name} ${p::class.simpleName} (${p.description})")
            }
        }
    }

    println("Have fun!")
    println()

    while (true) {
        print("You: ")

        when (val prompt = readln()) {
            "/bye" -> {
                break
            }

            "/clear" -> {
                bot.clearConversation()
            }

            "/h", "/history" -> {
                bot.conversation.value.messages.forEach { println(it) }
            }

            else -> {
                bot.chat(prompt).collect { update ->

                    when (update) {
                        is Llm.Update.Response -> {
                            update.content?.let {
                                print(it)
                            }
                        }

                        is Llm.Update.Stop -> {
                            println()
                        }

                        is Llm.Update.Info -> {
                            println(update.message)
                        }
                    }

                    System.out.flush()
                }
            }
        }
    }

    println()
    println("Bye!")
}

