package io.peekandpoke.crawl4ai

import de.peekandpoke.ultra.common.remote.buildUri
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class Crawl4aiClient(
    apiKey: String,
    private val baseUrl: String = "http://localhost:11235",
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
    },
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true // optional but usually helpful
        }
    }

    private val bearerToken = "Bearer $apiKey"

    suspend fun healthCheck(): Crawl4aiModels.HealthCheckResponse {
        val uri = buildUri("/health")
        val url = "$baseUrl$uri"

        val response = httpClient.get(url) {
            configureHeaders()
        }

        return response.body()
    }

    suspend fun crawlSync(
        url: String,
        config: JsonObject? = null,
    ): Crawl4aiModels.CrawlSyncResponse {
        val uri = buildUri("/crawl")
        val requestUrl = "$baseUrl$uri"

        val response = httpClient.post(requestUrl) {
            configureHeaders()

            contentType(ContentType.Application.Json)

            val body = buildJsonObject {
                putJsonArray("urls") {
                    add(url)
                }
                put("priority", 10)
            }.plus(config ?: buildJsonObject { })

            setBody(body)
        }

        val responseRawData = response.body<JsonObject>()

        // println(responseRawData)

        val responseData: Crawl4aiModels.CrawlSyncResponse = json.decodeFromJsonElement(responseRawData)

        return responseData
    }

    private fun HttpRequestBuilder.configureHeaders() {
        headers.append(HttpHeaders.Authorization, bearerToken)
    }
}
