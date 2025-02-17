package io.peekandpoke.aktor.tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.peekandpoke.aktor.examples.ExampleBot.rotX
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.OllamaModels
import java.net.HttpURLConnection.setFollowRedirects
import kotlin.time.Duration.Companion.seconds

/**
 * LLm Tool for https://ipinfo.io
 */
class IpInfoIo(
    private val token: String,
    private val httpClient: HttpClient = createDefaultHttpClient(),
): AutoCloseable {
    companion object {
        fun tool(token: String, ip: suspend () -> String? = { null }): Llm.Tool {
            val instance = IpInfoIo(token = token)

            return Llm.Tool(
                tool = OllamaModels.Tool.Function(
                    function = OllamaModels.Tool.Function.Data(
                        name = "get_detailed_location_info_IpInfoIo",
                        description = """
                            Gets detailed information about the user current location.
                            
                            Returns: 
                            JSON.
                        """.trimIndent(),
                        parameters = OllamaModels.AiType.AiObject(),
                    ),
                ),
                fn = { _ ->
                    instance.get(
                        ip()
                    )
                }
            )
        }

        fun createDefaultHttpClient(): HttpClient {
            return HttpClient(CIO) {
                engine {
                    requestTimeout = 180.seconds.inWholeMilliseconds
                    setFollowRedirects(true)
                }
            }
        }
    }

    override fun close() {
        httpClient.close()
    }

    suspend fun get(ip: String?): String {
        // https://ip-api.com/docs/api:json
        val response = httpClient.get("https://ipinfo.io/${ip ?: ""}?token=$token")

        val body = response.body<String>()

        return body
    }
}
