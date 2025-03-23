package io.peekandpoke.aktor.shared.llms.api

import de.peekandpoke.ultra.common.remote.*
import io.peekandpoke.aktor.shared.llms.model.RegisteredLlmModel
import kotlinx.coroutines.flow.Flow

class LlmsApiClient(config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/llms"

        val List = TypedApiEndpoint.Get(
            uri = BASE,
            response = RegisteredLlmModel.serializer().apiList(),
        )
    }

    fun list(): Flow<ApiResponse<List<RegisteredLlmModel>>> = call(
        List()
    )
}
