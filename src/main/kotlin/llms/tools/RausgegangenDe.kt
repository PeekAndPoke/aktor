package io.peekandpoke.aktor.llm.tools

import de.peekandpoke.ultra.common.datetime.MpClosedLocalDateRange
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.remote.buildUri
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.crawl4ai.Crawl4aiClient
import io.peekandpoke.crawl4ai.Crawl4aiModels
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RausgegangenDe(
    private val crawler: Crawl4aiClient,
) {
    fun asLlmTool(): Llm.Tool {
        return Llm.Tool.Function(
            name = "search_events_RausgegangenDe",
            description = """
                    Gets weather information about a location.
                    
                    Args:
                        city: The city
                        start_date: The start date as YYYY-MM-DD
                        end_date: The end date as YYYY-MM-DD (optional)
                        query: A query string (optional)                        
                    Returns: 
                        JSON
                """.trimIndent(),
            parameters = listOf(
                Llm.Tool.StringParam(
                    name = "city",
                    description = "The city",
                    required = true
                ),
                Llm.Tool.StringParam(
                    name = "start_date",
                    description = "The start date",
                    required = true
                ),
                Llm.Tool.StringParam(
                    name = "end_date",
                    description = "The end date",
                    required = false
                ),
                Llm.Tool.StringParam(
                    name = "query",
                    description = "A query string",
                    required = false
                ),
            ),
            fn = { params ->
                val city = params.getString("city")
                    ?: error("Missing parameter 'city'")

                val startDate = params.getString("start_date")?.let { MpLocalDate.tryParse(it) }
                    ?: error("Missing parameter 'start_date'")

                val endDate = params.getString("end_date")?.let { MpLocalDate.tryParse(it) }

                val query = params.getString("query")

                crawl(
                    city = city,
                    period = MpClosedLocalDateRange(startDate, endDate ?: startDate),
                    query = query,
                )
            }
        )
    }

    suspend fun crawl(city: String, period: MpClosedLocalDateRange, query: String? = null): String {

        // TODO: iterate all dates
        // TODO: what about the category?

        val page = 1

        val uri = buildUri("/{city}/eventsearch/") {
            set("city", city.lowercase())
            set("start_date__gte", period.from.format("yyyy-MM-dd"))
            set("start_date__lte", period.from.format("yyyy-MM-dd"))
            set("category", "")
            set("page", page)
        }

        val url = "https://rausgegangen.de$uri"

        val result = crawler.crawlAsync(
            url = url,
            config = buildJsonObject {
                put("css_selector", ".event-search-result-list")
                put("cache_mode", Crawl4aiModels.CacheMode.enabled.name)
            }
        ).await()

        return result.resultAsObj?.markdown
            ?: "Error: no result found for city '$city' and period '$period'"
    }
}
