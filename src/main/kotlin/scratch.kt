package io.peekandpoke.aktor

import io.peekandpoke.crawl4ai.Crawl4aiClient

suspend fun main() {

    val crawl4ai = Crawl4aiClient(apiKey = "")

    val result = crawl4ai.crawlSync("https://www.rausgegangen.de/")

    println(result)
}
