package de.peekandpoke.aktor.frontend

import de.peekandpoke.kraft.jsbridges.decodeJwtAsMap
import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource
import de.peekandpoke.kraft.streams.Unsubscribe
import de.peekandpoke.kraft.streams.addons.persistInLocalStorage
import de.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.aktor.shared.appuser.model.AppUserModel
import io.peekandpoke.reaktor.auth.model.LoginRequest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlin.js.Date

object AuthState : Stream<AuthState.Data> {

    @Serializable
    data class Data(
        val token: String?,
        val tokenUserId: String?,
        val tokenExpires: String?,
        val claims: String?,
        val user: AppUserModel?,
        val permissions: UserPermissions,
    ) {
        companion object {
            val empty = Data(
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

    private val streamSource = StreamSource<Data>(Data.empty).persistInLocalStorage("auth", Data.serializer())

    var redirectAfterLoginUri: String? = null

    override fun invoke(): Data = streamSource()

    override fun subscribeToStream(sub: (Data) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    suspend fun loginWithPassword(user: String, password: String): Data {
        val response = Apis.auth
            .login(LoginRequest.EmailAndPassword(email = user, password = password))
            .map { it.data }
            .catch { streamSource(Data.empty) }
            .firstOrNull()

        response?.let {
            val data = readJwt(
                token = it.token,
                user = it.getTypedUser(AppUserModel.serializer())
            )

            streamSource(data)
        }

        return streamSource()
    }

    fun logout() {
        MainRouter.navToUri(Nav.login())
        streamSource(Data.empty)
    }

    private fun readJwt(token: String, user: AppUserModel): Data {
        val claims = decodeJwtAsMap(token)

        // extract the permission from the token
        // TODO: needs to be configurable
        val namespace = "permissions"

        @Suppress("UNCHECKED_CAST")
        val permissions = UserPermissions(
            organisations = (claims["$namespace/organisations"] as? List<String> ?: emptyList()).toSet(),
            branches = (claims["$namespace/branches"] as? List<String> ?: emptyList()).toSet(),
            groups = (claims["$namespace/groups"] as? List<String> ?: emptyList()).toSet(),
            roles = (claims["$namespace/roles"] as? List<String> ?: emptyList()).toSet(),
            permissions = (claims["$namespace/permissions"] as? List<String> ?: emptyList()).toSet(),
        )

        val expDate = (claims["exp"] as? Int)?.let { Date(it.toLong() * 1000) }

        val userId = claims["sub"] as? String ?: ""

        return Data(
            token = token,
            tokenUserId = userId,
            tokenExpires = expDate?.toISOString(),
            claims = claims.toString(),
            user = user,
            permissions = permissions
        )
    }

}
