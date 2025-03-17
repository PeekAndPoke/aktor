package io.peekandpoke.crawl4ai

import de.peekandpoke.ultra.common.remote.buildUri
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    private val bearerToken = "Bearer $apiKey"

    suspend fun healthCheck(): Crawl4aiModels.HealthCheckResponse {
        val uri = buildUri("/health")
        val url = "$baseUrl$uri"

        val response = httpClient.get(url) {
            configureHeaders()
        }

        return response.body()
    }

    suspend fun crawlAsync(
        url: String,
        config: JsonObject? = null,
        timeout: Duration = 60.seconds,
    ): Deferred<Crawl4aiModels.CrawlTaskStatus> {
        val uri = buildUri("/crawl")
        val requestUrl = "$baseUrl$uri"

        val result = CompletableDeferred<Crawl4aiModels.CrawlTaskStatus>()

        val response = httpClient.post(requestUrl) {
            configureHeaders()

            contentType(ContentType.Application.Json)

            val body = buildJsonObject {
                put("urls", url)
                put("priority", 10)
            }.plus(config ?: buildJsonObject { })

            setBody(body)
        }

        val task = response.body<Crawl4aiModels.CrawlAsyncResponse>()

        println("crawlAsync: response: $task")

        try {
            withTimeout(timeout) {
                while (true) {
                    val taskStatus = getTask(task.task_id)

                    if (taskStatus.status == "completed") {
                        result.complete(taskStatus)
                        break
                    }

                    delay(200)
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("crawlAsync: timeout: $e")
            result.completeExceptionally(e)
            return result
        }

        return result
    }

    suspend fun getTask(taskId: String): Crawl4aiModels.CrawlTaskStatus {
        val uri = buildUri("/task/{task}") {
            set("task", taskId)
        }

        val url = "$baseUrl$uri"

        val response = httpClient.get(url) {
            configureHeaders()
        }

        return response.body()
    }

    private fun HttpRequestBuilder.configureHeaders() {
        headers.append(HttpHeaders.Authorization, bearerToken)
    }

}
