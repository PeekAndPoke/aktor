package io.peekandpoke.aktor.tools

import de.peekandpoke.ultra.common.remote.buildUri
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.OllamaModels
import java.net.HttpURLConnection.setFollowRedirects
import kotlin.time.Duration.Companion.seconds

/**
 * LLm Tool for https://open-meteo.com/
 */
class OpenMeteoCom(
    private val httpClient: HttpClient = createDefaultHttpClient(),
) : AutoCloseable {
    companion object {
        fun tool(): Llm.Tool {
            val instance = OpenMeteoCom()

            return Llm.Tool(
                tool = OllamaModels.Tool.Function(
                    function = OllamaModels.Tool.Function.Data(
                        name = "get_weather_info_OpenMeteoCom",
                        description = """
                            Gets weather information about a location.
                            
                            Returns: 
                            JSON.
                        """.trimIndent(),
                        parameters = OllamaModels.AiType.AiObject(
                            properties = mapOf(
                                "latitude" to OllamaModels.AiType.AiString("The latitude of the location"),
                                "longitude" to OllamaModels.AiType.AiString("The longitude of the location"),
                            ),
                            required = listOf("latitude", "longitude"),
                        ),
                    ),
                ),
                fn = { params ->
                    val lat = params.params.getValue("latitude").toDouble()
                    val lng = params.params.getValue("longitude").toDouble()

                    instance.get(lat = lat, lng = lng)
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

    suspend fun get(lat: Double, lng: Double): String {
        // https://open-meteo.com/en/docs

        // https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m,weather_code&forecast_days=1&format=json&timeformat=unixtime

        val url = buildUri("https://api.open-meteo.com/v1/forecast") {
            set("latitude", lat.toString())
            set("longitude", lng.toString())
            set("hourly", "weather_code")
            set("format", "json")
            set("timeformat", "unixtime")
            set("forecast_days", "1")
        }

        val response = httpClient.get(url)

        val body = response.body<Map<String, Any?>>()

        val times = ((body["hourly"] as Map<String, Any?>)["time"] as List<Long>)
        val codes = ((body["hourly"] as Map<String, Any?>)["weather_code"] as List<Int>)

        val timeToCode = times.zip(codes).toMap()

        return timeToCode.toString()
    }
}
