package io.peekandpoke.aktor.llm.ollama

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import kotlinx.coroutines.flow.*
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

                defaultRequest {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                }
            }
        }
    }

    override fun close() {
        httpClient.close()
    }

    override fun chat(conversation: Mutable<AiConversation>, streaming: Boolean): Flow<Llm.Update> {
        return flow {
            suspend fun call(): List<OllamaModels.ChatResponse> {
                return httpClient.chat(
                    conversation = conversation.value,
                    tools = tools.map(),
                ).onEach {
//                    println(it)
                }.fold(emptyList()) { acc, msg ->
                    acc + msg
                }
            }

            var done = false

            while (!done) {
                val parts = call()

                val toolCalls = parts.flatMap { it.message.tool_calls ?: emptyList() }

                if (toolCalls.isNotEmpty()) {
                    toolCalls.forEach { toolCall ->
                        processToolCall(conversation, toolCall)
                    }
                } else {
                    val answer = parts
                        .mapNotNull { part -> part.message.content }
                        .joinToString("") { it }

                    emit(Llm.Update.Response(answer))

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
            coerceInputValues = true
            prettyPrint = true
        }

        // Example GET request
        val request = preparePost("$baseUrl/api/chat") {
            headers {
                append(HttpHeaders.Accept, "application/json")
            }

            val messages = conversation.mapMessages()

//            messages.forEach { msg -> println("Message: ${Json.encodeToString(msg)}")}

            val body = OllamaModels.ChatRequest(
                model = model,
                messages = messages,
                stream = false,
                tools = tools,
            )

            val bodyJson = json.encodeToString(OllamaModels.ChatRequest.serializer(), body)

            // println("Request: $bodyJson")

            setBody(bodyJson)
        }

        return flow {
            request.execute { response ->
                val ch = response.bodyAsChannel()

                while (!ch.isClosedForRead) {
                    val buffer = ch.readUTF8Line()
                    if (buffer != null) {
//                    println("Buffer: $buffer")

                        val obj = try {
                            json.decodeFromString<OllamaModels.ChatResponse>(buffer)
                        } catch (t: Throwable) {
                            println("Error decoding: $buffer")
                            throw t
                        }

//                    println("Decoded: $obj")

                        emit(obj)
                    }
                }
            }
        }
    }

    private suspend fun FlowCollector<Llm.Update>.processToolCall(
        conversation: Mutable<AiConversation>,
        toolCall: OllamaModels.ChatResponse.ToolCall,
    ) {
        toolCall.function.let { function ->

            val fnName = function.name
            val fnArgsRaw = function.arguments
            val fnArgs = AiConversation.Message.ToolCall.Args.ofMap(fnArgsRaw)
            val tool = tools.firstOrNull { it.name == fnName }

            emit(
                Llm.Update.Info(
                    "Tool call: ${function.name} | Args: ${fnArgs.print()} | ArgsRaw: $fnArgsRaw"
                )
            )

            val toolResult = when (tool) {
                null -> "Error! The tool ${function.name} is not available."
                else -> try {
                    tool.call(fnArgs)
                } catch (t: Throwable) {
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
                    AiConversation.Message.Assistant(
                        content = null,
                        toolCalls = listOf(
                            AiConversation.Message.ToolCall(
                                id = "",
                                name = fnName,
                                args = fnArgs,
                            )
                        )
                    )
                )
            }

            conversation.modify {
                it.add(
                    AiConversation.Message.Tool(
                        content = toolResult,
                        toolCall = AiConversation.Message.ToolCall(id = fnName, name = fnName, args = fnArgs),
                    )
                )
            }
        }
    }

    @JvmName("mapLlmTools")
    private fun List<Llm.Tool>.map() = map { tool ->
        when (tool) {
            is Llm.Tool.Function -> OllamaModels.Tool.Function(
                function = OllamaModels.Tool.Function.Data(
                    name = tool.name,
                    description = tool.description,
                    parameters = OllamaModels.ObjectType(
                        properties = tool.parameters.associate { p ->
                            p.name to when (p) {
                                is Llm.Tool.StringParam -> OllamaModels.StringType(description = p.description)
                                is Llm.Tool.IntegerParam -> OllamaModels.StringType(description = p.description)
                                is Llm.Tool.BooleanParam -> OllamaModels.BooleanType(description = p.description)
                            }
                        },
                        required = tool.parameters.filter { p -> p.required }.map { p -> p.name }
                    ),
                )
            )
        }
    }

    private fun AiConversation.mapMessages() = messages.map { msg ->
        when (msg) {
            is AiConversation.Message.System ->
                OllamaModels.Message.System(content = msg.content)

            is AiConversation.Message.User ->
                OllamaModels.Message.User(content = msg.content)

            is AiConversation.Message.Assistant ->
                OllamaModels.Message.Assistant(content = msg.content.orEmpty(), tool_calls = msg.toolCalls?.map())

            is AiConversation.Message.Tool ->
                OllamaModels.Message.Tool(name = msg.toolCall.name, content = msg.content)
        }
    }

    @JvmName("mapToolCalls")
    private fun List<AiConversation.Message.ToolCall>.map(): List<OllamaModels.Message.ToolCall>? =
        map { call -> call.map() }.takeIf { it.isNotEmpty() }

    private fun AiConversation.Message.ToolCall.map(): OllamaModels.Message.ToolCall {
        return OllamaModels.Message.ToolCall(
            function = OllamaModels.Message.ToolCall.Function(
                name = name,
                arguments = args.toMap()
            )
        )
    }
}
