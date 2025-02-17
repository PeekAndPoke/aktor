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
 * LLm Tool for https://ip-api.com
 */
class IpApiCom(
    private val httpClient: HttpClient = createDefaultHttpClient(),
): AutoCloseable {
    companion object {
        fun tool(ip: suspend () -> String? = { null }): Llm.Tool {
            val instance = IpApiCom()

            return Llm.Tool(
                tool = OllamaModels.Tool.Function(
                    function = OllamaModels.Tool.Function.Data(
                        name = "get_detailed_location_info_IpApiCom",
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
        val response = httpClient.get("http://ip-api.com/json/${ip ?: ""}")

        val body = response.body<String>()

        return body
    }
}
