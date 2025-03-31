package io.peekandpoke.aktor

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.funktor.auth.AuthStorage
import de.peekandpoke.funktor.auth.ReaktorAuth
import de.peekandpoke.funktor.auth.db.karango.KarangoAuthRecordsRepo
import de.peekandpoke.funktor.auth.db.karango.KarangoAuthStorage
import de.peekandpoke.funktor.core.App
import de.peekandpoke.funktor.core.installKontainer
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.funktor
import de.peekandpoke.funktor.rest.auth.jwtUserProvider
import de.peekandpoke.karango.karango
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
import io.peekandpoke.aktor.backend.llms.LlmRegistry
import io.peekandpoke.aktor.backend.llms.LlmServices
import io.peekandpoke.aktor.backend.llms.api.LlmApiFeature
import io.peekandpoke.aktor.cli.CommandLineChatCli
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.ollama.OllamaLlm
import io.peekandpoke.aktor.llm.ollama.OllamaModels
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

inline val KontainerAware.llms: LlmServices get() = kontainer.get()
inline val ApplicationCall.llms: LlmServices get() = kontainer.llms
inline val RoutingContext.llms: LlmServices get() = call.llms

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
    funktor(
        config = config,
        rest = {
            jwt(
                JwtConfig(
                    singingKey = config.auth.apiJwtSigningKey,
                    permissionsNs = "permissions",
                    userNs = "user",
                    issuer = "https://api.aktor.io",
                    audience = "api.aktor.io",
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

    module(ReaktorAuth)
    // TODO: add config builder to ReaktorAuth() to enable karango storage
    dynamic(AuthStorage::class, KarangoAuthStorage::class)
    dynamic(KarangoAuthRecordsRepo::class)
    dynamic(KarangoAuthRecordsRepo.Fixtures::class)

    // Auth-Realms
    dynamic(AppUserAuthenticationRealm::class)

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

    // AppUser services
    singleton(AppUserServices::class)
    singleton(AppUserApiFeature::class)

    singleton(AppUsersRepo::class)
    singleton(AppUsersRepo.Fixtures::class)

    singleton(AiConversationsRepo::class)
    singleton(AiConversationsRepo.Fixtures::class)

    // Llm Services
    singleton(LlmServices::class)
    singleton(LlmApiFeature::class)
    singleton(LlmRegistry::class) { keys: KeysConfig ->
        val default = LlmRegistry.RegisteredLlm(
            id = "openai/gpt-4o-mini",
            description = "OpenAI GPT-4o mini",
            llm = OpenAiLlm(
                model = "gpt-4o-mini",
                authToken = keys.config.getString("OPENAI_API_KEY"),
            )
        )

        LlmRegistry(default = default)
            .plus(
                id = "openai/gpt-4o",
                description = "OpenAI GPT-4o",
                llm = OpenAiLlm(
                    model = "gpt-4o",
                    authToken = keys.config.getString("OPENAI_API_KEY"),
                )
            ).plus(
                id = "openai/o3-mini",
                description = "OpenAI o3-mini",
                llm = OpenAiLlm(
                    model = "o3-mini",
                    authToken = keys.config.getString("OPENAI_API_KEY"),
                )
            ).plus(
                id = "ollama/llama3.2:1b",
                description = "Ollama Llama 3.2 1B",
                llm = OllamaLlm(
                    model = OllamaModels.LLAMA_3_2_1B,
                )
            ).plus(
                id = "ollama/llama3.2:3b",
                description = "Ollama Llama 3.2 3B",
                llm = OllamaLlm(
                    model = OllamaModels.LLAMA_3_2_3B,
                )
            ).plus(
                id = "ollama/qwen2.5:7b",
                description = "Ollama Qwen 2.5 7B",
                llm = OllamaLlm(
                    model = OllamaModels.QWEN_2_5_7B,
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
