package io.peekandpoke.aktor.mcpclient

import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlin.time.Duration

typealias OnMcpMessage = suspend (JSONRPCMessage) -> Unit

/**
 * Client transport for SSE: this will connect to a server using Server-Sent Events for receiving
 * messages and make separate POST requests for sending messages.
 */
class CustomSSEClientTransport(
    private val client: HttpClient,
    private val baseUrl: String,
    private val connectUrl: String,
    private val reconnectionTime: Duration? = null,
    private val requestBuilder: HttpRequestBuilder.() -> Unit = {},
) : Transport {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val McpJson: Json by lazy {
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
                classDiscriminatorMode = ClassDiscriminatorMode.NONE
                explicitNulls = false
            }
        }
    }

    private class State(
        val initialized: AtomicBoolean = atomic(false),
        val endpoint: CompletableDeferred<String> = CompletableDeferred<String>(),
    ) {
        var session: ClientSSESession? = null
        var job: Job? = null

        val scope by lazy {
            CoroutineScope(session!!.coroutineContext + SupervisorJob())
        }

        fun cancel() {
            endpoint.cancel()
            session?.cancel()
            job?.cancel()
        }
    }

    private var state = State()

    var onDisconnect: (suspend () -> Unit)? = null

    override var onClose: (() -> Unit)? = null
    override var onError: ((Throwable) -> Unit)? = null
    override var onMessage: OnMcpMessage? = null

    override suspend fun start() {
        if (!state.initialized.compareAndSet(false, true)) {
            return
        }

        println("[MCPClient] connecting to $connectUrl")

        try {
            state.session = client.sseSession(
                urlString = connectUrl,
                reconnectionTime = reconnectionTime,
                block = requestBuilder,
            )

            startLoop()

        } catch (e: Exception) {
            println("[MCPClient] connection failed: $e")
            close()
            onDisconnect?.invoke()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun send(message: JSONRPCMessage) {
        if (!state.endpoint.isCompleted) {
            error("Not connected")
        }

        try {
            val response = client.post(state.endpoint.getCompleted()) {
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(McpJson.encodeToString(message))
            }

            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                error("Error POSTing to endpoint (HTTP ${response.status}): $text")
            }
        } catch (e: Exception) {
            onError?.invoke(e)
            throw e
        }
    }

    override suspend fun close() {
        state.cancel()
        state = State()
    }

    private suspend fun startLoop() {

        state.job = state.scope.launch(CoroutineName("SseTransport#${hashCode()}")) {
            state.session!!.incoming
                .onCompletion { cause ->
                    println("[MCPClient] session closed: $cause")
                    onDisconnect?.invoke()
                }.collect { event ->
                    when (event.event) {
                        "error" -> {
                            val e = IllegalStateException("SSE error: ${event.data}")
                            onError?.invoke(e)
                            throw e
                        }

                        "open" -> {
                            // The connection is open, but we need to wait for the endpoint to be received.
                        }

                        "endpoint" -> {
                            println("[MCPClient] endpoint received: ${event.data}")

                            try {
                                val eventData = event.data ?: ""
                                // check url correctness
                                val maybeEndpoint = Url("${baseUrl.trimEnd('/')}/${eventData.trimStart('/')}")

                                state.endpoint.complete(maybeEndpoint.toString())
                            } catch (e: Exception) {
                                onError?.invoke(e)
                                close()
                                error(e)
                            }
                        }

                        else -> {
                            try {
                                val message = McpJson.decodeFromString<JSONRPCMessage>(event.data ?: "")
                                onMessage?.invoke(message)
                            } catch (e: Exception) {
                                onError?.invoke(e)
                            }
                        }
                    }
                }

            println("[MCPClient] session closed")
        }

        state.endpoint.await()
    }
}
