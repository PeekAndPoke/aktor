@file:Suppress("EnumEntryName")

package io.peekandpoke.crawl4ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Suppress("PropertyName")
object Crawl4aiModels {

    val x = Json.Default

    @Serializable
    data class HealthCheckResponse(
        val status: String,
        val available_slots: Int,
        val memory_usage: Double,
        val cpu_usage: Double,
    )

    @Serializable
    data class CrawlAsyncResponse(
        val task_id: String,
    )

    /**
     * Defines the caching behavior for web crawling operations.
     *
     * Modes:
     * - ENABLED: Normal caching behavior (read and write)
     * - DISABLED: No caching at all
     * - READ_ONLY: Only read from cache, don't write
     * - WRITE_ONLY: Only write to cache, don't read
     * - BYPASS: Bypass cache for this operation
     */
    enum class CacheMode {
        enabled,
        disabled,
        read_only,
        write_only,
        bypass,
    }

    @Serializable
    data class CrawlTaskStatus(
        val status: String,
        val created_at: Double,
        val result: JsonObject? = null,
    ) {
        val resultAsObj by lazy {
            result?.let { x.decodeFromJsonElement(WebpageCrawlResult.serializer(), it) }
        }
    }

    @Serializable
    data class WebpageCrawlResult(
        val url: String,
        val html: String,
        val success: Boolean,
        val cleaned_html: String,
        val media: Media? = null,
        val links: Links? = null,
        val downloaded_files: String? = null,
        val screenshot: String? = null,
        val markdown: String,
        val markdown_v2: MarkdownV2?,
        val fit_markdown: String,
        val fit_html: String,
        val extracted_content: String? = null,
        val metadata: JsonObject? = null,
        val error_message: String? = null,
        val session_id: String? = null,
        val response_headers: JsonObject,
        val status_code: Int,
    ) {
        @Serializable
        data class Media(
            val images: List<JsonObject>? = null,
            val videos: List<JsonObject>? = null,
            val audios: List<JsonObject>? = null,
        )

        @Serializable
        data class Links(
            val internal: List<JsonObject>? = null,
            val external: List<JsonObject>? = null,
        )

        @Serializable
        data class MarkdownV2(
            val raw_markdown: String,
            val markdown_with_citations: String,
            val references_markdown: String,
            val fit_markdown: String,
            val fit_html: String,
        )
    }
}


