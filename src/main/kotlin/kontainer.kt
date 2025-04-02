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
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.senders.ExampleDomainsIgnoringEmailSender
import de.peekandpoke.funktor.messaging.senders.aws.AwsSesSender
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
import io.peekandpoke.aktor.backend.appuser.AppUserModule
import io.peekandpoke.aktor.backend.llms.LlmServices
import io.peekandpoke.aktor.cli.CommandLineChatCli
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llms.LlmsModule
import io.peekandpoke.aktor.llms.tools.*
import io.peekandpoke.crawl4ai.Crawl4aiClient
import io.peekandpoke.geo.GeoModule
import io.peekandpoke.geo.TimeShape
import java.io.File

data class KeysConfig(val config: Config)

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

    // Mailing
    val awsSender: AwsSesSender by lazy {
        AwsSesSender.of(config = config.aws.ses)
    }

    singleton(EmailSender::class) {
        ExampleDomainsIgnoringEmailSender(
            wrapped = awsSender,
        )
    }

    module(ReaktorAuth)
    // TODO: add config builder to ReaktorAuth() to enable karango storage
    dynamic(AuthStorage::class, KarangoAuthStorage::class)
    dynamic(KarangoAuthRecordsRepo::class)
    dynamic(KarangoAuthRecordsRepo.Fixtures::class)

    // Mount Karango
    karango(config = config.arangodb)

    // Keys config
    instance(
        KeysConfig(
            ConfigFactory.parseFile(File("./config/keys.env.conf"))
        )
    )

    // Apps
    singleton(ApiApp::class)

    // Modules
    module(AppUserModule)
    module(LlmsModule)

    // AppUser services

    singleton(AiConversationsRepo::class)
    singleton(AiConversationsRepo.Fixtures::class)

    // Llm Services

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
