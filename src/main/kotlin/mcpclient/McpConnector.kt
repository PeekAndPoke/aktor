package io.peekandpoke.aktor.mcpclient

import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.shared.DEFAULT_REQUEST_TIMEOUT
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.peekandpoke.aktor.mcpclient.McpConnector.Companion.JsonCodec
import io.peekandpoke.aktor.mcpclient.McpConnector.Companion.toJson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

private val LOGGER = LoggerFactory.getLogger("McpConnector")

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
    }

    val isConnected: Boolean

    suspend fun connect()
}

typealias ProgressHandler = (Progress) -> Unit
typealias ResponseHandler = (JSONRPCResponse?, Throwable?) -> Unit

class SseMcpConnector(
    val baseUrl: String = "http://localhost:8000",
    val connectUri: String = "/sse",
    val httpClient: HttpClient = HttpClient {
        install(SSE) {
            reconnectionTime = 1.seconds
            maxReconnectionAttempts = Int.MAX_VALUE
        }
    },
) : McpConnector {

    class RequestHandler(
        val response: ResponseHandler,
        val progress: ProgressHandler?,
    )

    private val handlers = mutableMapOf<RequestId, RequestHandler>()

    override val isConnected: Boolean get() = state is Connected

    private var state: State = NotConnected()

    sealed interface State {
        val session: ClientSSESession?
    }

    abstract inner class SenderState() : State {
        abstract val endpointUrl: String

        suspend fun send(message: JSONRPCMessage) {
            try {
                val response = httpClient.post(endpointUrl) {
                    headers.append(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(JsonCodec.encodeToString(message))
                }

                if (!response.status.isSuccess()) {
                    val text = response.bodyAsText()
                    error("Error POSTing to endpoint (HTTP ${response.status}): $text")
                }
            } catch (e: Exception) {
                // TODO: what do we do with the error?
                throw e
            }
        }
    }

    private inner class NotConnected : State {
        override val session: ClientSSESession? = null
    }

    private inner class Connecting(
        override val session: ClientSSESession,
    ) : State

    private inner class Connected(
        override val session: ClientSSESession,
        override val endpointUrl: String,
    ) : SenderState()

    override suspend fun connect() {
        if (state !is NotConnected) {
            return
        }

        val session = httpClient.sseSession(baseUrl + connectUri)

        state = Connecting(session)

        listen(session)
    }

    suspend fun <T : RequestResult> request(
        request: Request,
        options: RequestOptions? = null,
    ): T {
        LOGGER.trace("Sending request: ${request.method}")

        val message = request.toJson()
        val messageId = message.id
        val result = CompletableDeferred<T>()

        result.invokeOnCompletion {
            handlers.remove(messageId)
        }

        val responseHandler: ResponseHandler = handler@{ response, error ->
            if (error != null) {
                result.completeExceptionally(error)
                return@handler
            }

            if (response?.error != null) {
                result.completeExceptionally(IllegalStateException(response.error.toString()))
                return@handler
            }

            try {
                @Suppress("UNCHECKED_CAST")
                result.complete(response!!.result as T)
            } catch (error: Throwable) {
                result.completeExceptionally(error)
            }
        }

        handlers[messageId] = RequestHandler(response = responseHandler, progress = options?.onProgress)

        val timeout = options?.timeout ?: DEFAULT_REQUEST_TIMEOUT

        try {
            withTimeout(timeout) {
                LOGGER.trace("Sending request message with id: $messageId")
                send(message)
            }
            return result.await()
        } catch (cause: TimeoutCancellationException) {
            LOGGER.error("Request timed out after ${timeout.inWholeMilliseconds}ms: ${request.method}")

            val mcpError = McpError(
                code = ErrorCode.Defined.RequestTimeout.code,
                message = "Request timed out",
                data = JsonObject(
                    mapOf("timeout" to JsonPrimitive(timeout.inWholeMilliseconds))
                )
            )

            // Notify the server
            send(
                CancelledNotification(requestId = messageId, reason = mcpError.message)
            )

            result.cancel(cause)

            throw cause
        }
    }

    private suspend fun send(notification: ClientNotification) {
        send(notification.toJson())
    }

    private suspend fun send(message: JSONRPCMessage) {
        (state as? Connected)?.send(message)
    }

    private suspend fun listen(session: ClientSSESession) {
        session.incoming
            .onCompletion { cause ->
                println("Session closed: $cause")
                // TODO: what now?
            }.collect { event ->
                println("Received event: ${event.event}")
                println(event.toString())

                when (event.event) {
                    "error" -> {
                        val e = IllegalStateException("SSE error: ${event.data}")
                        // TODO: What now?
                        throw e
                    }

                    "open" -> {
                        // The connection is open, but we need to wait for the endpoint to be received.
                    }

                    "endpoint" -> {
                        println("[MCPClient] endpoint received: ${event.data}")

                        val eventData = event.data ?: ""
                        // check url correctness
                        val endpoint = "${baseUrl.trimEnd('/')}/${eventData.trimStart('/')}"

                        state = Connected(session, endpoint)

                        send(InitializedNotification())
                    }

                    else -> {
                        try {
                            val message = JsonCodec.decodeFromString<JSONRPCMessage>(event.data ?: "")
                            // TODO: what now?
                        } catch (e: Exception) {
                            // TODO: what now?
                        }
                    }
                }
            }

        LOGGER.info("Session closed")
    }
}
