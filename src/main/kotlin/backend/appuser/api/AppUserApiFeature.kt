package io.peekandpoke.aktor.backend.appuser.api

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiFeature
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ultra.vault.Stored
import io.peekandpoke.aktor.backend.aiconversation.api.AppUserConversationsApi
import io.peekandpoke.aktor.backend.appuser.AppUser

class AppUserApiFeature(converter: OutgoingConverter) : ApiFeature {

    interface AppUserAware {
        val user: Stored<AppUser>
    }

    data class AppUserParam(
        override val user: Stored<AppUser>,
    ) : AppUserAware

    data class SearchForAppUserParam(
        override val user: Stored<AppUser>,
        val epp: Int = 50,
        val page: Int = 1,
        val query: String? = null,
    ) : AppUserAware

    override val name = "AppUsers"

    override val description = """
        Exposes api endpoints for app users.
    """.trimIndent()

    val sseApi = AppUserSseApi(converter)
    val conversationsApi = AppUserConversationsApi(converter)

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        sseApi,
        conversationsApi,
    )
}
