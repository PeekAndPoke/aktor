package io.peekandpoke.aktor.llms.mcp.client

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface McpConnector {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val JsonCodec: Json by lazy {
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
                classDiscriminatorMode = ClassDiscriminatorMode.NONE
                explicitNulls = false
            }
        }

        fun Request.toJson(): JSONRPCRequest {
            return JSONRPCRequest(
                method = method.value,
                params = JsonCodec.encodeToJsonElement(this),
                jsonrpc = JSONRPC_VERSION,
            )
        }

        fun ClientNotification.toJson(): JSONRPCNotification {
            return JSONRPCNotification(
                method = method.value,
                params = JsonCodec.encodeToJsonElement(this),
            )
        }

        fun CallToolRequest.Companion.of(name: String, args: Map<String, Any?>): CallToolRequest {
            val jsonArguments = args.mapValues { (_, value) ->
                when (value) {
                    is String -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    is JsonElement -> value
                    null -> JsonNull
                    else -> JsonPrimitive(value.toString())
                }
            }

            return CallToolRequest(
                name = name,
                arguments = JsonObject(jsonArguments),
            )
        }
    }

    val isConnected: Boolean

    suspend fun connect(timeout: Duration = 10.seconds)

    suspend fun close()

    suspend fun <T : RequestResult> request(
        request: Request,
        options: RequestOptions? = null,
    ): T

    suspend fun listTools(
        options: RequestOptions? = null,
    ): ListToolsResult? {
        return request(
            ListToolsRequest()
        )
    }

    suspend fun callTool(
        name: String,
        args: Map<String, Any?>,
        options: RequestOptions? = null,
    ): CallToolResultBase? {
        val request = CallToolRequest.of(name, args)

        return request(request = request, options = options)
    }
}

