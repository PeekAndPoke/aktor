package io.peekandpoke.aktor.shared.credentials.api

import de.peekandpoke.ultra.common.remote.*
import io.peekandpoke.aktor.shared.credentials.model.GoogleOAuthFlow
import io.peekandpoke.aktor.shared.credentials.model.UserCredentialsModel

class CredentialsApiClient(config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/credentials/{user}"

        val List = TypedApiEndpoint.Get(
            uri = "$BASE/list",
            response = UserCredentialsModel.serializer().apiList(),
        )

        val InitGoogleFlow = TypedApiEndpoint.Post(
            uri = "$BASE/google/init",
            body = GoogleOAuthFlow.InitRequest.serializer(),
            response = GoogleOAuthFlow.InitResponse.serializer().api(),
        )

        val CompleteGoogleFlow = TypedApiEndpoint.Post(
            uri = "$BASE/google/complete",
            body = GoogleOAuthFlow.CompleteRequest.serializer(),
            response = GoogleOAuthFlow.CompleteResponse.serializer().api(),
        )
    }

    fun list(user: String) = call(
        List(
            "user" to user,
        )
    )

    fun initGoogleFlow(user: String, scopes: List<String>, redirectUri: String) = call(
        InitGoogleFlow(
            "user" to user,
            body = GoogleOAuthFlow.InitRequest(
                scopes = scopes,
                redirectUri = redirectUri,
            ),
        )
    )

    fun completeGoogleFlow(user: String, state: String, code: String) = call(
        CompleteGoogleFlow(
            "user" to user,
            body = GoogleOAuthFlow.CompleteRequest(
                state = state,
                code = code,
            ),
        )
    )
}
