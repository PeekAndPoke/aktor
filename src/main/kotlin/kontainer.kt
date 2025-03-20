package io.peekandpoke.aktor

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.karango.karango
import de.peekandpoke.ktorfx.core.App
import de.peekandpoke.ktorfx.core.installKontainer
import de.peekandpoke.ktorfx.core.kontainer
import de.peekandpoke.ktorfx.core.model.InsightsConfig
import de.peekandpoke.ktorfx.ktorFx
import de.peekandpoke.ktorfx.rest.auth.jwtUserProvider
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.security.jwt.JwtConfig
import de.peekandpoke.ultra.vault.profiling.DefaultQueryProfiler
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.aktor.api.ApiApp
import io.peekandpoke.aktor.api.AppUserApiFeature
import io.peekandpoke.aktor.backend.AppUserServices
import io.peekandpoke.aktor.backend.AppUsersRepo
import io.peekandpoke.aktor.cli.CommandLineChatCli
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.tools.*
import io.peekandpoke.crawl4ai.Crawl4aiClient
import io.peekandpoke.geo.GeoModule
import io.peekandpoke.geo.TimeShape
import java.io.File

data class KeysConfig(val config: Config)

inline val KontainerAware.appUsers: AppUserServices get() = kontainer.get()
inline val ApplicationCall.appUsers: AppUserServices get() = kontainer.appUsers
inline val RoutingContext.appUsers: AppUserServices get() = call.appUsers

fun Route.installApiKontainer(app: App<AktorConfig>, insights: InsightsConfig?) = installKontainer { call ->
    app.kontainers.create {
        // user record provider
        with { call.jwtUserProvider() }
        // Insights config
        insights?.let { with { insights } }
        // Database query profile
        if (app.config.arangodb.flags.enableProfiler) {
            with {
                DefaultQueryProfiler(explainQueries = app.config.arangodb.flags.enableExplain)
            }
        }
    }
}


fun createBlueprint(config: AktorConfig) = kontainer {

    // Mount all KtorFx things
    ktorFx(
        config = config,
        rest = {
            jwt(
                JwtConfig(
                    singingKey = config.auth.apiJwtSigningKey,
                    permissionsNs = config.auth.apiJwtPermissionsNs,
                    userNs = "user",
                    issuer = "https://api.jointhebase.co",
                    audience = "api.jointhebase.co",
                )
            )
        },
        logging = {
            useKarango()
        },
        cluster = {
            useKarango()
        },
        messaging = {
            useKarango()
        }
    )

    // Mount Karango
    karango(config = config.arangodb)

    // singleton
    singleton(KeysConfig::class) {
        KeysConfig(
            ConfigFactory.parseFile(File("./config/keys.env"))
        )
    }

    // Apps
    singleton(ApiApp::class)

    // App services
    singleton(AppUserServices::class)
    singleton(AppUserApiFeature::class)
    singleton(AppUsersRepo::class)
    singleton(AppUsersRepo.Fixtures::class)

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
