package io.peekandpoke.aktor.llms.tools

import com.openmeteo.api.Forecast
import com.openmeteo.api.OpenMeteo
import com.openmeteo.api.common.Response
import com.openmeteo.api.common.Response.ExperimentalGluedUnitTimeStepValues
import com.openmeteo.api.common.time.Date
import com.openmeteo.api.common.time.Time
import com.openmeteo.api.common.time.Timezone
import com.openmeteo.api.common.units.TemperatureUnit
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.peekandpoke.aktor.llms.Llm
import io.peekandpoke.aktor.utils.append
import io.peekandpoke.geo.TimeShape
import kotlinx.serialization.json.*
import java.net.HttpURLConnection.setFollowRedirects
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds


/**
 * LLm Tool for https://open-meteo.com/
 */
class OpenMeteoCom(
    private val kronos: Kronos,
    private val timeshape: TimeShape,
    private val httpClient: HttpClient = createDefaultHttpClient(),
) : AutoCloseable {
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        private val timeFormat = SimpleDateFormat("HH:mm")

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

    @OptIn(ExperimentalGluedUnitTimeStepValues::class)
    private class ResultBuilder(
        private val initAction: JsonObjectBuilder.(time: Time) -> Unit,
    ) {
        companion object {
            fun daily() = ResultBuilder { time ->
                put("date", dateFormat.format(time))
            }

            fun hourly() = ResultBuilder { time ->
                put("date", dateFormat.format(time))
                put("time", timeFormat.format(time))
            }
        }

        private val results = mutableMapOf<Time, JsonObject>()

        fun get(time: Time) = results.getOrPut(time) {
            buildJsonObject {
                initAction(time)
            }
        }

        fun append(time: Time, builderAction: JsonObjectBuilder.() -> Unit) {
            results[time] = get(time).append(builderAction)
        }

        fun appendAll(
            values: () -> Response.UnitTimeStepValues,
            builderAction: JsonObjectBuilder.(value: Double?) -> Unit,
        ): ResultBuilder = apply {

            try {
                values().values.forEach { (time, value) ->
                    append(time) {
                        builderAction(value)
                    }
                }
            } catch (e: Exception) {
                // TODO: real logging
                println("Error while processing values: $e")
                e.printStackTrace()
            }
        }

        fun results(): List<JsonObject> = results.toList()
            .sortedBy { (time, _) -> time }
            .map { (_, value) -> value }
    }

    private val jsonPrinter = Json { prettyPrint = true }

    fun asLlmTools(): List<Llm.Tool> {
        return listOf(
            asDailyForecastTool(),
            asHourlyForecastTool(),
        )
    }

    fun asDailyForecastTool(): Llm.Tool {
        return Llm.Tool.Function(
            name = "weather-daily-forecast-OpenMeteoCom",
            description = """
                Get basic weather forcast for the next days and the given location.                                   
                
                Returns: 
                    JSON
                """.trimIndent(),
            parameters = listOf(
                Llm.Tool.StringParam(
                    name = "lat",
                    description = "The latitude of the location",
                    required = true
                ),
                Llm.Tool.StringParam(
                    name = "lng",
                    description = "The longitude of the location",
                    required = true
                ),
                Llm.Tool.StringParam(
                    name = "start_date",
                    description = "The start date of the forecast (yyyy-MM-dd) (defaults to today)",
                    required = false,
                ),
                Llm.Tool.IntegerParam(
                    name = "days",
                    description = "The number of days for the forecast (defaults to 7)",
                    required = false,
                )
            ),
            fn = { params ->
                val lat = params.getDouble("lat") ?: error("Missing parameter 'lat'")
                val lng = params.getDouble("lng") ?: error("Missing parameter 'lng'")

                val tz = timeshape.getTimeZone(lat = lat, lng = lng)

                val startDate = params.getString("start_date")
                    ?.let { MpLocalDate.tryParse(it) ?: error("Invalid start_date: $it") }
                    ?: kronos.localDateNow(tz)

                val days = params.getInt("days") ?: 7

                getDailyForecast(lat = lat, lng = lng, startDate = startDate, days = days)
            }
        )
    }

    fun asHourlyForecastTool(): Llm.Tool {
        return Llm.Tool.Function(
            name = "weather-hourly-forecast-OpenMeteoCom",
            description = """
                Get detailed hourly weather forcast for the given date and location.                    
                Prefer this tools, when the user ask about the weather for a specific date.
                
                Returns: 
                    JSON
            """.trimIndent(),
            parameters = listOf(
                Llm.Tool.StringParam(
                    name = "lat",
                    description = "The latitude of the location",
                    required = true,
                ),
                Llm.Tool.StringParam(
                    name = "lng",
                    description = "The longitude of the location",
                    required = true,
                ),
                Llm.Tool.StringParam(
                    name = "date",
                    description = "The date of the forecast (yyyy-MM-dd) (defaults to today)",
                    required = false,
                )
            ),
            fn = { params ->
                val lat = params.getDouble("lat") ?: error("Missing parameter 'lat'")
                val lng = params.getDouble("lng") ?: error("Missing parameter 'lng'")

                val tz = timeshape.getTimeZone(lat = lat, lng = lng)

                val date = params.getString("date")
                    ?.let { MpLocalDate.tryParse(it) ?: error("Invalid date: $it") }
                    ?: kronos.localDateNow(tz)

                getHourlyForecast(lat = lat, lng = lng, date = date)
            }
        )
    }

    override fun close() {
        httpClient.close()
    }

    @OptIn(ExperimentalGluedUnitTimeStepValues::class)
    fun getDailyForecast(lat: Double, lng: Double, startDate: MpLocalDate, days: Int): String {

        val om = OpenMeteo(
            latitude = lat.toFloat(),
            longitude = lng.toFloat(),
        )

        val parameters = listOf(
            Forecast.Daily.temperature2mMin,
            Forecast.Daily.temperature2mMax,
            Forecast.Daily.weathercode,
            Forecast.Daily.rainSum,
            Forecast.Daily.snowfallSum,
        )

        val tz = timeshape.getTimeZone(lat = lat, lng = lng)

        val todayPlus14 = kronos.zonedDateTimeNow(tz).atStartOfDay().plus(14.days)
        val startDT = startDate.atStartOfDay(tz)
        val start = Date(minOf(todayPlus14, startDT).toEpochMillis())
        val end = Date(minOf(todayPlus14, startDT.plus((days - 1).days)).toEpochMillis())

        val forecast = om.forecast {
            this.startDate = start
            this.endDate = end
            this.daily = Forecast.Daily { parameters }
            this.temperatureUnit = TemperatureUnit.Celsius
            this.timezone = Timezone.getTimeZone(id = tz.id)
        }.getOrThrow()

        val results = ResultBuilder.daily()

        results.appendAll({ forecast.daily.getValue(Forecast.Daily.temperature2mMax) }) {
            put("max-temperature", it)
        }

        results.appendAll({ forecast.daily.getValue(Forecast.Daily.temperature2mMin) }) {
            put("min-temperature", it)
        }

        results.appendAll({ forecast.daily.getValue(Forecast.Daily.weathercode) }) {
            put("weather", weatherCodes[it?.toInt()] ?: "n/a")
        }

        results.appendAll({ forecast.daily.getValue(Forecast.Daily.rainSum) }) {
            put("rain", it)
        }

        results.appendAll({ forecast.daily.getValue(Forecast.Daily.snowfallSum) }) {
            put("snowfall", it)
        }

        return jsonPrinter.encodeToString(results.results())
    }

    @OptIn(ExperimentalGluedUnitTimeStepValues::class)
    fun getHourlyForecast(lat: Double, lng: Double, date: MpLocalDate): String {
        val om = OpenMeteo(
            latitude = lat.toFloat(),
            longitude = lng.toFloat(),
        )

        val parameters = listOf(
            Forecast.Hourly.temperature2m,
            Forecast.Hourly.weathercode,
            Forecast.Hourly.rain,
            Forecast.Hourly.snowfall,
        )

        val tz = timeshape.getTimeZone(lat = lat, lng = lng)
        val omDate = Date(date.atStartOfDay(tz).toEpochMillis())

        val forecast = om.forecast {
            this.startDate = omDate
            this.endDate = omDate
            this.hourly = Forecast.Hourly { parameters }
            this.temperatureUnit = TemperatureUnit.Celsius
            this.timezone = Timezone.getTimeZone(id = tz.id)
        }.getOrThrow()

        val results = ResultBuilder.hourly()

        results.appendAll({ forecast.hourly.getValue(Forecast.Hourly.temperature2m) }) {
            put("temperature", it)
        }

        results.appendAll({ forecast.hourly.getValue(Forecast.Hourly.weathercode) }) {
            put("weather", weatherCodes[it?.toInt()] ?: "n/a")
        }

        results.appendAll({ forecast.hourly.getValue(Forecast.Hourly.rain) }) {
            put("rain", it)
        }

        results.appendAll({ forecast.hourly.getValue(Forecast.Hourly.snowfall) }) {
            put("snowfall", it)
        }

        return jsonPrinter.encodeToString(results.results())
    }
}
