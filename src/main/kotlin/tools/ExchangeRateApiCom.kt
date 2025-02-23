package io.peekandpoke.aktor.tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.jackson.*
import io.peekandpoke.aktor.llm.Llm
import java.net.HttpURLConnection.setFollowRedirects
import kotlin.time.Duration.Companion.seconds

/**
 * LLm Tool for https://ip-api.com
 */
class ExchangeRateApiCom(
    private val httpClient: HttpClient = createDefaultHttpClient(),
) : AutoCloseable {
    companion object {
        fun tool(ip: suspend () -> String? = { null }): Llm.Tool {
            val instance = ExchangeRateApiCom()

            return Llm.Tool.Function(
                name = "get_exchange_rate_ExchangeRateApiCom",
                description = """
                    Gets exchange rate between two currencies.
                    
                    Returns: 
                    JSON.
                """.trimIndent(),
                parameters = listOf(
                    Llm.Tool.StringParam(name = "from", description = "Source currency code", required = true),
                    Llm.Tool.StringParam(name = "to", description = "Target currency code", required = true),
                ),
                fn = { params ->
                    val from = params.getString("from") ?: error("Missing parameter 'from'")
                    val to = params.getString("to") ?: error("Missing parameter 'to'")

                    val rate = instance.get(from = from.uppercase(), to = to.uppercase())

                    rate?.toString() ?: "Error: no exchange rate found from $from to $to"
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
                    jackson {  }
                }
            }
        }
    }

    @Suppress("PropertyName")
    data class LatestRates(
        val result: String,
        val provider: String,
        val documentation: String,
        val terms_of_use: String,
        val time_last_update_unix: Long,
        val time_last_update_utc: String,
        val time_next_update_unix: Long,
        val time_next_update_utc: String,
        val time_eol_unix: Int,
        val base_code: String,
        val rates: Map<String, Double>
    )


    override fun close() {
        httpClient.close()
    }

    suspend fun get(from: String, to: String): Double? {
        // https://ip-api.com/docs/api:json
        val response = httpClient.get("https://open.er-api.com/v6/latest/$from")

        val body = response.body<LatestRates>()

        return body.rates[to]
    }
}
