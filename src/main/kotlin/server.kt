package io.peekandpoke.aktor

import ch.qos.logback.classic.Level
import de.peekandpoke.ktorfx.cluster.workers.launchWorkers
import de.peekandpoke.ktorfx.core.lifecycle.lifeCycle
import de.peekandpoke.ktorfx.insights.instrumentWithInsights
import de.peekandpoke.ktorfx.logging.karango.addKarangoAppender
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.peekandpoke.aktor.api.ApiApp
import io.peekandpoke.aktor.llm.mcp.client.McpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

var mcp: McpClient? = null

@Suppress("unused")
fun Application.module() = app.module(this) { app, config, init ->

    launch {
        delay(1000)
        try {
            mcp = McpClient(
                name = "Play",
                version = "1.0.0",
                toolNamespace = "playground",
            )

            mcp!!.connect()
        } catch (e: Exception) {
            println("Failed to connect to MCP: $e")
        }
    }


    // Add log appender that writes to the database
    addKarangoAppender(config = config.arangodb, minLevel = Level.INFO)

    if (!config.ktor.isTest) {
        lifeCycle(init) {
            // TODO: start and stop the workers by using LifeCycleHooks as well
            launchWorkers { init.clone() }
        }
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

        ////  API  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        host("api.*".toRegex()) {
            // Install Kontainer into ApplicationCall
            installApiKontainer(app, config.api.insights)
            // Instrument the pipeline with insights collectors
            instrumentWithInsights()
            // Mount the app
            init.get(ApiApp::class).apply { mountApiAppModule() }
        }
    }
}
