package io.peekandpoke.crawl4ai

import de.peekandpoke.ultra.kontainer.module

val Crawl4AiModule = module { config: Crawl4AiConfig ->
    dynamic(Crawl4aiClient::class) {
        Crawl4aiClient(
            apiKey = config.apiKey
        )
    }
}
