package io.peekandpoke.aktor

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.ktorfx.ktorFx
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.aktor.app.AppUserRepo
import io.peekandpoke.aktor.cli.CommandLineChatCli
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.tools.*
import io.peekandpoke.crawl4ai.Crawl4aiClient
import io.peekandpoke.geo.GeoModule
import io.peekandpoke.geo.TimeShape
import java.io.File

data class KeysConfig(val config: Config)

fun createBlueprint(config: AktorConfig) = kontainer {

    // Mount all KtorFx things
    ktorFx(
        config = config,
    )

    // singleton
    singleton(KeysConfig::class) {
        KeysConfig(
            ConfigFactory.parseFile(File("./config/keys.env"))
        )
    }

    // App services
    singleton(AppUserRepo::class)
    singleton(AppUserRepo.Fixtures::class)

    // /////////////////////////////////////////////////////////////////////////////////

    // TODO: create kontainer module
    dynamic(Crawl4aiClient::class) { keys: KeysConfig ->
        Crawl4aiClient(
            apiKey = keys.config.getString("CRAWL4AI_API_KEY")
        )
    }

    module(GeoModule)

    // LLM Tools
    dynamic(ExchangeRateApiCom::class) { ExchangeRateApiCom.default }
    dynamic(IpApiCom::class) { IpApiCom.default }
    dynamic(IpInfoIo::class) { keys: KeysConfig ->
        IpInfoIo(keys.config.getString("IP_INFO_API_KEY"))
    }
    dynamic(MathParserTool::class)
    dynamic(OpenMeteoCom::class) { kronos: Kronos, timeshape: TimeShape ->
        OpenMeteoCom(kronos = kronos, timeshape = timeshape)
    }
    dynamic(RausgegangenDe::class)

    // CLI commands
    dynamic(CommandLineChatCli::class)

    // Example bots
    singleton(ExampleBots::class)
}
