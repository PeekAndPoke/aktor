package io.peekandpoke.aktor.tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.peekandpoke.aktor.llm.Llm
import java.net.HttpURLConnection.setFollowRedirects
import kotlin.time.Duration.Companion.seconds

/**
 * LLm Tool for https://ip-api.com
 */
class IpApiCom(
    private val httpClient: HttpClient = createDefaultHttpClient(),
) : AutoCloseable {
    companion object {
        val default = IpApiCom()

        fun createDefaultHttpClient(): HttpClient {
            return HttpClient(CIO) {
                engine {
                    requestTimeout = 180.seconds.inWholeMilliseconds
                    setFollowRedirects(true)
                }
            }
        }
    }

    fun asLlmTool(ip: suspend () -> String? = { null }): Llm.Tool {
        return Llm.Tool.Function(
            name = "get_detailed_location_info_IpApiCom",
            description = """
                Gets detailed information about the user current location.
                
                Returns: 
                    JSON.
            """.trimIndent(),
            parameters = emptyList(),
            fn = { _ ->
                get(
                    ip()
                )
            }
        )
    }

    override fun close() {
        httpClient.close()
    }

    suspend fun get(ip: String?): String {
        // https://ip-api.com/docs/api:json
        val response = httpClient.get("http://ip-api.com/json/${ip ?: ""}")

        val body = response.body<String>()

        return body
    }
}
