package io.peekandpoke.aktor.tools

import de.peekandpoke.ultra.common.datetime.MpTimezone
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import de.peekandpoke.ultra.common.remote.buildUri
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.jackson.*
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
        /**
         * WMO Weather interpretation codes (WW)
         *
         * Code	Description
         *
         * 0            Clear sky
         * 1, 2, 3      Mainly clear, partly cloudy, and overcast
         * 45, 48       Fog and depositing rime fog
         * 51, 53, 55   Drizzle: Light, moderate, and dense intensity
         * 56, 57       Freezing Drizzle: Light and dense intensity
         * 61, 63, 65   Rain: Slight, moderate and heavy intensity
         * 66, 67       Freezing Rain: Light and heavy intensity
         * 71, 73, 75   Snow fall: Slight, moderate, and heavy intensity
         * 77           Snow grains
         * 80, 81, 82   Rain showers: Slight, moderate, and violent
         * 85, 86       Snow showers slight and heavy
         * 95 *         Thunderstorm: Slight or moderate
         * 96, 99 *     Thunderstorm with slight and heavy hail
         *
         * (*) Thunderstorm forecast with hail is only available in Central Europe
         */
        val weatherCodes = mapOf(
            0 to "Clear sky",
            1 to "Mainly clear",
            2 to "Partly cloudy",
            3 to "Overcast",
            45 to "Fog",
            48 to "Depositing rime fog",
            51 to "Drizzle: Light intensity",
            53 to "Drizzle: Moderate intensity",
            55 to "Drizzle: Dense intensity",
            56 to "Freezing Drizzle: Light intensity",
            57 to "Freezing Drizzle: Dense intensity",
            61 to "Rain: Slight intensity",
            63 to "Rain: Moderate intensity",
            65 to "Rain: Heavy intensity",
            66 to "Freezing Rain: Light intensity",
            67 to "Freezing Rain: Heavy intensity",
            71 to "Snow fall: Slight intensity",
            73 to "Snow fall: Moderate intensity",
            75 to "Snow fall: Heavy intensity",
            77 to "Snow grains",
            80 to "Rain showers: Slight",
            81 to "Rain showers: Moderate",
            82 to "Rain showers: Violent",
            85 to "Snow showers: Slight",
            86 to "Snow showers: Heavy",
            95 to "Thunderstorm: Slight or moderate",
            96 to "Thunderstorm with slight hail",
            99 to "Thunderstorm with heavy hail"
        )

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

                install(ContentNegotiation) {
                    jackson {

                    }
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

        val timeToCode = times.zip(codes)
            .map { (time, code) ->
                MpZonedDateTime.fromEpochSeconds(time, timezone = MpTimezone.UTC) to weatherCodes[code]
            }.map { (time, code) ->
                "${time.format("HH:mm")} - ${code ?: "unknown"}"
            }.joinToString("\n")

        return timeToCode
    }
}
