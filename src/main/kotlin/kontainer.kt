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
import io.peekandpoke.aktor.backend.aiconversation.AiConversationsRepo
import io.peekandpoke.aktor.backend.appuser.AppUserServices
import io.peekandpoke.aktor.backend.appuser.AppUsersRepo
import io.peekandpoke.aktor.backend.appuser.api.AppUserApiFeature
import io.peekandpoke.aktor.cli.CommandLineChatCli
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.LlmRegistry
import io.peekandpoke.aktor.llm.ollama.OllamaLlm
import io.peekandpoke.aktor.llm.openai.OpenAiLlm
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
    singleton(AppUserApiFeature::class)

    // App services
    singleton(AppUserServices::class)

    singleton(AppUsersRepo::class)
    singleton(AppUsersRepo.Fixtures::class)

    singleton(AiConversationsRepo::class)
    singleton(AiConversationsRepo.Fixtures::class)

    // /////////////////////////////////////////////////////////////////////////////////

    singleton(LlmRegistry::class) { keys: KeysConfig ->

        val default = LlmRegistry.RegisteredLlm(
            id = "openai/gpt-4o-mini",
            description = "OpenAI GPT-4 Mini",
            llm = OpenAiLlm(
                model = "gpt-4o-mini",
                authToken = keys.config.getString("OPENAI_API_KEY"),
            )
        )

        LlmRegistry(default = default).plus(
            id = "openai/gpt-4",
            description = "OpenAI GPT-4",
            llm = OpenAiLlm(
                model = "gpt-4",
                authToken = keys.config.getString("OPENAI_API_KEY"),
            )
        ).plus(
            id = "ollama/llama3.2:1b",
            description = "Ollama Llama 3.2 1B",
            llm = OllamaLlm(
                model = "ollama/llama3.2:1b",
            )
        ).plus(
            id = "ollama/llama3.2:3b",
            description = "Ollama Llama 3.2 3B",
            llm = OllamaLlm(
                model = "ollama/llama3.2:3b",
            )
        )
    }

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
