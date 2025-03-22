package io.peekandpoke.aktor.llm.mcp.client

import de.peekandpoke.ultra.common.ellipsis
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.shared.DEFAULT_REQUEST_TIMEOUT
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.peekandpoke.aktor.llm.mcp.McpProgressHandler
import io.peekandpoke.aktor.llm.mcp.McpResponseHandler
import io.peekandpoke.aktor.llm.mcp.client.McpConnector.Companion.toJson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

private val LOGGER = LoggerFactory.getLogger("SseMcpConnector")

@Suppress("LoggingStringTemplateAsArgument")
class SseMcpConnector(
    val baseUrl: String = "http://localhost:8000",
    val connectUri: String = "/sse",
    val httpClient: HttpClient = HttpClient {
        install(SSE)

        install(HttpTimeout) {
            this.socketTimeoutMillis = 60 * 60 * 1000L
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
        val response: McpResponseHandler,
        val progress: McpProgressHandler?,
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

        val message = request.toJson()
        val messageId = message.id
        val result = CompletableDeferred<T>()

        LOGGER.debug("Sending request: ${request.method} with id $messageId")

        result.invokeOnCompletion {
            handlers.remove(messageId)
        }

        val responseHandler: McpResponseHandler = handler@{ response, error ->
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
        LOGGER.debug("Sending: $message")
        (state as? Connected)?.send(message)
    }

    private fun listen(session: ClientSSESession): Job {
        val ctx = session.coroutineContext + SupervisorJob()
        val scope = CoroutineScope(ctx)


        return scope.launch(CoroutineName("McpClient#${hashCode()}")) {

            var running = true

            launch {
                while (running) {
                    delay(5000)
                    LOGGER.trace("Sending ping")
                    val result = request<EmptyRequestResult>(PingRequest())
                    LOGGER.trace("Ping result: $result")
                }
            }

            session.incoming.onCompletion { cause ->
                println("Session closed with reason: $cause")
                // TODO: what now? schedule reconnect?
            }.collect { event ->
                LOGGER.debug("Received: ${event.event} | ${event.data?.ellipsis(80)}")

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
                        LOGGER.info("Endpoint received: ${event.data}")

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
                                scope.launch {
                                    handlers[response.id]?.response?.invoke(response, null)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // TODO: what now?
                        }
                    }
                }
            }

            running = false

            LOGGER.info("Session closed $session")
        }
    }
}
