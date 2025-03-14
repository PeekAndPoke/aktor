package de.peekandpoke.aktor.frontend

import de.peekandpoke.ultra.common.remote.buildUri
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.peekandpoke.aktor.model.AiConversation
import kotlinx.serialization.json.Json

/**
 * Client for interacting with the chat API
 */
class ChatClient(private val baseUrl: String = "http://localhost:8081") {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * Sends a message to the chat endpoint
     *
     * @param message The message to send to the chat service
     * @return The response from the server
     */
    suspend fun sendMessage(message: String): AiConversation {
        val uri = buildUri("/chat/{message}") {
            set("message", message)
        }
        var url = "$baseUrl$uri"

        return client.get(url).body()
    }

    /**
     * Closes the HTTP client resources
     */
    fun close() {
        client.close()
    }
}
