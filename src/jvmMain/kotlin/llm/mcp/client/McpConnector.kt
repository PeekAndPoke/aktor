package io.peekandpoke.aktor.llm.mcp.client

import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.shared.DEFAULT_REQUEST_TIMEOUT
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.peekandpoke.aktor.llm.mcp.client.McpConnector.Companion.toJson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.future.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
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

    sealed interface State {
        suspend fun close()
    }

    abstract inner class SenderState() : State {
        abstract val endpointUrl: String

        suspend fun send(message: JSONRPCMessage) {
            try {
                val response = httpClient.post(endpointUrl) {
                    headers.append(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(McpConnector.Companion.JsonCodec.encodeToString(message))
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
        override suspend fun close() {}
    }

    private inner class Connecting() : State {
        override suspend fun close() {}
    }

    private inner class Handshake(
        val onConnected: CompletableFuture<Unit>,
        val sseSessionJob: Job,
    ) : State {
        override suspend fun close() {
            sseSessionJob.cancel()
        }
    }

    private inner class Connected(
        val sseSessionJob: Job,
        override val endpointUrl: String,
    ) : SenderState() {
        override suspend fun close() {
            sseSessionJob.cancel()
        }
    }

    class RequestHandler(
        val response: ResponseHandler,
        val progress: ProgressHandler?,
    )

    private val handlers = mutableMapOf<RequestId, RequestHandler>()

    override val isConnected: Boolean get() = state is Connected

    private var state: State = NotConnected()

    override suspend fun connect(timeout: Duration) {
        if (state !is NotConnected) {
            return
        }

        val onConnected = CompletableFuture<Unit>()

        try {
            withTimeout(timeout) {
                state = Connecting()

                val session = httpClient.sseSession(baseUrl + connectUri)
                val job = listen(session)

                state = Handshake(onConnected, job)
            }
        } catch (e: TimeoutCancellationException) {
            onConnected.completeExceptionally(e)
            throw e
        }

        onConnected.await()
    }

    override suspend fun close() {
        state.close()
        state = NotConnected()
    }

    override suspend fun <T : RequestResult> request(
        request: Request,
        options: RequestOptions?,
    ): T {
        if (!isConnected) {
            LOGGER.error("Not connected. Trying to send request: ${request.method}")
        }

        LOGGER.info("Sending request: ${request.method}")

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
                LOGGER.info("Sending request message with id: $messageId")
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
        LOGGER.info("Sending: $message")
        (state as? Connected)?.send(message)
    }

    private suspend fun listen(session: ClientSSESession): Job {
        val ctx = session.coroutineContext +
                Dispatchers.IO +
                SupervisorJob() +
                CoroutineName("McpClient#${hashCode()}")

        val scope = CoroutineScope(ctx)

        return scope.launch {
            session.incoming
                .onCompletion { cause ->
                    println("Session closed: $cause")
                    // TODO: what now?
                }.collect { event ->
                    LOGGER.info("Received: ${event.event}")
                    LOGGER.info(event.toString())

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
                            LOGGER.info("[MCPClient] endpoint received: ${event.data}")

                            val eventData = event.data ?: ""
                            // check url correctness
                            val endpoint = "${baseUrl.trimEnd('/')}/${eventData.trimStart('/')}"

                            when (val s = state) {
                                is Handshake -> {
                                    state = Connected(s.sseSessionJob, endpoint)

                                    send(InitializedNotification())

                                    s.onConnected.complete(Unit)
                                }

                                else -> {
                                    // ignore
                                }
                            }
                        }

                        else -> {
                            try {
                                val message =
                                    McpConnector.Companion.JsonCodec.decodeFromString<JSONRPCMessage>(event.data ?: "")

                                (message as? JSONRPCResponse)?.let { response ->
                                    handlers[response.id]?.response?.invoke(response, null)
                                }
                            } catch (e: Exception) {
                                // TODO: what now?
                            }
                        }
                    }
                }

            LOGGER.info("Session closed")
        }
    }
}
