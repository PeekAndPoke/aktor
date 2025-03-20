package io.peekandpoke.aktor

import com.fasterxml.jackson.annotation.JsonIgnore
import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.core.config.ktor.KtorConfig
import de.peekandpoke.ktorfx.core.config.ktorfx.KtorFxConfig
import de.peekandpoke.ktorfx.core.model.InsightsConfig

data class AktorConfig(
    override val ktor: KtorConfig,
    override val ktorFx: KtorFxConfig = KtorFxConfig(),
    val auth: AuthConfig,
    val arangodb: ArangoDbConfig,
    val api: ApiConfig,
) : AppConfig {

    data class AuthConfig(
        @JsonIgnore
        val apiJwtSigningKey: String,
        val apiJwtPermissionsNs: String = "permissions",
        val apiJwtUserNs: String = "user",
    )

    data class ApiConfig(
        val baseUrl: String,
        val insights: InsightsConfig = InsightsConfig(),
    )
}
