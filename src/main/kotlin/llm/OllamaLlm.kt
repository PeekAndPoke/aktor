package io.peekandpoke.aktor.llm

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import io.peekandpoke.aktor.model.OllamaModels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection.setFollowRedirects
import kotlin.time.Duration.Companion.seconds

class OllamaLlm(
    override val model: String,
    override val tools: List<Llm.Tool>,
    private val baseUrl: String = "http://127.0.0.1:11434",
    private val httpClient: HttpClient = createDefaultHttpClient(),
) : Llm, AutoCloseable {
    companion object {
        fun createDefaultHttpClient(): HttpClient {
            return HttpClient(CIO) {
                engine {
                    requestTimeout = 180.seconds.inWholeMilliseconds
                    setFollowRedirects(true)
                }
            }
        }
    }

    override fun close() {
        httpClient.close()
    }

    override fun chat(conversation: Mutable<AiConversation>): Flow<Llm.Update> {

        return flow {
            suspend fun call(): List<OllamaModels.ChatResponse> {
                return httpClient.chat(
                    conversation.value,
                    tools.map { it.tool },
                ).onEach {
                    emit(Llm.Update.Response(it))
                }.fold(emptyList()) { acc, msg -> acc + msg }
            }

            var done = false
            var answer: String?

            while (!done) {
                val parts = call()

                val toolCalls = parts.flatMap { it.message.tool_calls ?: emptyList() }

                if (toolCalls.isNotEmpty()) {
                    toolCalls.forEach { toolCall ->
                        toolCall.function.let { function ->

                            emit(
                                Llm.Update.Info(
                                    "Tool call: ${function.name} | Args: ${function.arguments}"
                                )
                            )

                            val tool = tools.firstOrNull { it.tool.name == function.name }

                            val params = Llm.Tool.Params(function.arguments ?: emptyMap())

                            val toolResult = when (tool) {
                                null -> "Error! The tool ${function.name} is not available."
                                else -> try {
                                    tool.fn(params)
                                } catch(t: Throwable) {
                                    "Error! Calling the tool ${function.name} failed: ${t.message}."
                                }
                            }

                            emit(
                                Llm.Update.Info(
                                    "Tool result: $toolResult"
                                )
                            )

                            conversation.modify {
                                it.add(
                                    AiConversation.Message.Tool(name = function.name, content = toolResult)
                                )
                            }
                        }
                    }
                } else {
                    answer = parts
                        .mapNotNull { part -> part.message.content?.takeIf { it.isNotBlank() } }
                        .joinToString("") { it }

                    conversation.modify {
                        it.add(AiConversation.Message.Assistant(content = answer))
                    }

                    done = true
                }
            }

            emit(Llm.Update.Stop)
        }
    }

    private suspend fun HttpClient.chat(
        conversation: AiConversation,
        tools: List<OllamaModels.Tool>,
    ): Flow<OllamaModels.ChatResponse> {

        @Suppress("OPT_IN_USAGE")
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            prettyPrint = true
        }

        // Example GET request
        val request = preparePost("$baseUrl/api/chat") {
            headers {
                append(HttpHeaders.Accept, "application/json")
            }

            val body = OllamaModels.ChatRequest(
                model = model,
                messages = conversation.messages,
                stream = false,
                tools = tools,
            )

            val bodyJson = json.encodeToString(OllamaModels.ChatRequest.serializer(), body)

//        println("Request: $bodyJson")

            setBody(bodyJson)
        }

        return flow {
            request.execute { response ->
                val ch = response.bodyAsChannel()

                while (!ch.isClosedForRead) {
                    val buffer = ch.readUTF8Line()
                    if (buffer != null) {
//                    println("Buffer: $buffer")

                        val obj = json.decodeFromString<OllamaModels.ChatResponse>(buffer)

//                    println("Decoded: $obj")

                        emit(obj)
                    }
                }
            }
        }
    }
}
