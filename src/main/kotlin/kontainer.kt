package io.peekandpoke.aktor

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.funktor.core.App
import de.peekandpoke.funktor.core.installKontainer
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.funktor
import de.peekandpoke.funktor.insights.instrumentWithInsights
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.senders.ExampleDomainsIgnoringEmailSender
import de.peekandpoke.funktor.messaging.senders.aws.AwsSesSender
import de.peekandpoke.funktor.rest.auth.jwtUserProvider
import de.peekandpoke.karango.karango
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.kontainer
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.aktor.api.ApiApp
import io.peekandpoke.aktor.backend.aiconversation.AiConversationsRepo
import io.peekandpoke.aktor.backend.appuser.AppUserModule
import io.peekandpoke.aktor.backend.credentials.CredentialsConfig
import io.peekandpoke.aktor.backend.credentials.CredentialsModule
import io.peekandpoke.aktor.backend.llms.LlmServices
import io.peekandpoke.aktor.cli.CommandLineChatCli
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llms.LlmsModule
import io.peekandpoke.aktor.llms.tools.*
import io.peekandpoke.crawl4ai.Crawl4AiConfig
import io.peekandpoke.crawl4ai.Crawl4AiModule
import io.peekandpoke.geo.GeoModule
import io.peekandpoke.geo.TimeShape
import java.io.File

data class KeysConfig(val config: Config)

inline val KontainerAware.llms: LlmServices get() = kontainer.get()
inline val ApplicationCall.llms: LlmServices get() = kontainer.llms
inline val RoutingContext.llms: LlmServices get() = call.llms

fun Route.installApiKontainer(app: App<AktorConfig>, insights: InsightsConfig?) {
    installKontainer { call ->
        app.kontainers.create {
            // user record provider
            with { call.jwtUserProvider() }
            // Insights config
            insights?.let { with { insights } }
        }
    }

    instrumentWithInsights(insights)
}

fun createBlueprint(config: AktorConfig) = kontainer {
    // Mount all KtorFx things
    funktor(
        config = config,
        rest = {
            jwt(config.auth.jwt)
        },
        logging = {
            useKarango()
        },
        cluster = {
            useKarango()
        },
        messaging = {
            useKarango()
        },
        auth = {
            useKarango()
        }
    )

    // Mount Karango
    karango(config = config.arangodb)

    // Keys config
    val keys = KeysConfig(
        ConfigFactory.parseFile(File("./config/keys.env.conf"))
    )

    instance(keys)

    // Mailing
    val awsSender: AwsSesSender by lazy {
        AwsSesSender.of(config = config.aws.ses)
    }

    singleton(EmailSender::class) {
        ExampleDomainsIgnoringEmailSender(
            wrapped = awsSender,
        )
    }

    // Apps
    singleton(ApiApp::class)

    // Modules
    module(AppUserModule)
    module(
        CredentialsModule, CredentialsConfig(
            googleClientId = keys.config.getString("GOOGLE_SSO_CLIENT_ID"),
            googleClientSecret = keys.config.getString("GOOGLE_SSO_CLIENT_SECRET"),
            googleClientAppName = "Aktor",
        )
    )
    module(LlmsModule)

    // AppUser services

    singleton(AiConversationsRepo::class)
    singleton(AiConversationsRepo.Fixtures::class)

    // Llm Services
    module(Crawl4AiModule, Crawl4AiConfig(apiKey = keys.config.getString("CRAWL4AI_API_KEY")))
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
