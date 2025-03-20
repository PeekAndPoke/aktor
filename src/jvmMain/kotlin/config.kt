package io.peekandpoke.aktor

import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.core.config.ktor.KtorConfig
import de.peekandpoke.ktorfx.core.config.ktorfx.KtorFxConfig

data class AktorConfig(
    override val ktor: KtorConfig,
    override val ktorFx: KtorFxConfig = KtorFxConfig(),
    val arangodb: ArangoDbConfig,
) : AppConfig {
}
