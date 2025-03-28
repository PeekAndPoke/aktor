package io.peekandpoke.reaktor.auth

import de.peekandpoke.kraft.addons.routing.Router
import de.peekandpoke.kraft.addons.routing.routerMiddleware
import de.peekandpoke.kraft.jsbridges.decodeJwtAsMap
import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource
import de.peekandpoke.kraft.streams.Unsubscribe
import de.peekandpoke.kraft.streams.addons.persistInLocalStorage
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.slumber.JsonUtil.toJsonObject
import io.peekandpoke.reaktor.auth.api.AuthApiClient
import io.peekandpoke.reaktor.auth.model.LoginRequest
import io.peekandpoke.reaktor.auth.model.LoginResponse
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.js.Date

inline fun <reified USER> authState(
    api: AuthApiClient,
) = AuthState<USER>(
    userSerializer = serializer(),
    api = api,
)

class AuthState<USER>(
    val userSerializer: KSerializer<USER>,
    val api: AuthApiClient,
) : Stream<AuthState.Data<USER>> {

    @Serializable
    data class Data<USER>(
        val token: LoginResponse.Token?,
        val tokenUserId: String?,
        val tokenExpires: String?,
        val claims: JsonObject?,
        val user: USER?,
        val permissions: UserPermissions,
    ) {
        companion object {
            fun <USER> empty() = Data<USER>(
                token = null,
                tokenUserId = null,
                tokenExpires = null,
                claims = null,
                user = null,
                permissions = UserPermissions()
            )
        }

        val isLoggedIn get() = token != null && user != null

        val isNotLoggedIn get() = !isLoggedIn

        val loggedInUser get() = user.takeIf { isLoggedIn }
    }

    private val streamSource = StreamSource<Data<USER>>(Data.empty())
        .persistInLocalStorage("auth", Data.serializer(userSerializer))

    private var redirectAfterLoginUri: String? = null

    override fun invoke(): Data<USER> = streamSource()

    override fun subscribeToStream(sub: (Data<USER>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    fun routerMiddleWare(loginUri: String) = routerMiddleware {
        val auth = invoke()

        if (!auth.isLoggedIn) {
            redirectAfterLoginUri = uri.takeIf { it != loginUri }
            redirectTo(loginUri)
        }
    }

    fun redirectAfterLogin(router: Router, defaultUri: String) {
        router.navToUri(redirectAfterLoginUri ?: defaultUri)
    }

    suspend fun loginWithPassword(user: String, password: String): Data<USER> {
        val response = api
            .login(LoginRequest.EmailAndPassword(email = user, password = password))
            .map { it.data }
            .catch { streamSource(Data.empty()) }
            .firstOrNull()

        response?.let {
            val data = readJwt(
                token = it.token,
                user = it.getTypedUser(userSerializer)
            )

            streamSource(data)
        }

        return streamSource()
    }

    fun logout() {
        streamSource(Data.empty())
    }

    private fun readJwt(token: LoginResponse.Token, user: USER): Data<USER> {
        val claims = decodeJwtAsMap(token.token)

        // extract the permission from the token
        val permissions = token.permissionsNs.let { ns ->
            @Suppress("UNCHECKED_CAST")
            UserPermissions(
                organisations = (claims["$ns/organisations"] as? List<String> ?: emptyList()).toSet(),
                branches = (claims["$ns/branches"] as? List<String> ?: emptyList()).toSet(),
                groups = (claims["$ns/groups"] as? List<String> ?: emptyList()).toSet(),
                roles = (claims["$ns/roles"] as? List<String> ?: emptyList()).toSet(),
                permissions = (claims["$ns/permissions"] as? List<String> ?: emptyList()).toSet(),
            )
        }

        val expDate = (claims["exp"] as? Int)?.let { Date(it.toLong() * 1000) }

        val userId = claims["sub"] as? String ?: ""

        return Data(
            token = token,
            tokenUserId = userId,
            tokenExpires = expDate?.toISOString(),
            claims = claims.toJsonObject(),
            permissions = permissions,
            user = user
        )
    }
}
