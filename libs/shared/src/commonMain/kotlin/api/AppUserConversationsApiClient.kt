package io.peekandpoke.aktor.shared.api

import de.peekandpoke.ultra.common.remote.*
import io.peekandpoke.aktor.shared.model.AiConversationModel
import io.peekandpoke.aktor.shared.model.AiConversationRequest

class AppUserConversationsApiClient(config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/app-user/{user}/conversations"

        val List = TypedApiEndpoint.Get(
            uri = BASE,
            response = AiConversationModel.serializer().apiPaged(),
        )

        val Create = TypedApiEndpoint.Put(
            uri = "$BASE/create",
            body = AiConversationRequest.Create.serializer(),
            response = AiConversationModel.serializer().api(),
        )

        val Get = TypedApiEndpoint.Get(
            uri = "$BASE/{conversation}",
            response = AiConversationModel.serializer().api(),
        )

        val Send = TypedApiEndpoint.Put(
            uri = "$BASE/{conversation}/send",
            body = AiConversationRequest.Send.serializer(),
            response = AiConversationRequest.Response.serializer().api(),
        )
    }

    fun list(user: String, epp: Int = 50, page: Int = 1) = call(
        List(
            "user" to user,
            "epp" to epp.toString(),
            "page" to page.toString(),
        )
    )

    fun create(user: String, request: AiConversationRequest.Create = AiConversationRequest.Create) = call(
        Create(
            "user" to user,
            body = request,
        )
    )

    fun get(user: String, conversation: String) = call(
        Get(
            "user" to user,
            "conversation" to conversation,
        )
    )

    fun send(user: String, conversation: String, message: AiConversationRequest.Send) = call(
        Send(
            "user" to user,
            "conversation" to conversation,
            body = message,
        )
    )
}
