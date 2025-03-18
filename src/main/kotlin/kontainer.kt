package io.peekandpoke.aktor

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.tools.*
import io.peekandpoke.crawl4ai.Crawl4aiClient
import io.peekandpoke.geo.GeoModule
import io.peekandpoke.geo.TimeShape
import java.io.File

data class KeysConfig(val config: Config)

val blueprint = kontainer {

    // singleton
    singleton(KeysConfig::class) {
        KeysConfig(
            ConfigFactory.parseFile(File("./config/keys.conf"))
        )
    }

    // Utils
    dynamic(Kronos::class) { Kronos.systemUtc }

    // TODO: create kontainer module
    dynamic(Crawl4aiClient::class) { keys: KeysConfig ->
        Crawl4aiClient(
            apiKey = keys.config.getString("keys.CRAWL4AI_TOKEN")
        )
    }

    module(GeoModule)

    // LLM Tools
    dynamic(ExchangeRateApiCom::class) { ExchangeRateApiCom.default }
    dynamic(IpApiCom::class) { IpApiCom.default }
    dynamic(IpInfoIo::class) { keys: KeysConfig ->
        IpInfoIo(keys.config.getString("keys.IP_INFO_TOKEN"))
    }
    dynamic(OpenMeteoCom::class) { kronos: Kronos, timeshape: TimeShape ->
        OpenMeteoCom(kronos = kronos, timeshape = timeshape)
    }
    dynamic(RausgegangenDe::class)

    // Example bots
    singleton(ExampleBots::class)
}
