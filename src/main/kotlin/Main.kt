package io.peekandpoke.aktor

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.peekandpoke.aktor.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection.setFollowRedirects
import kotlin.time.Duration.Companion.seconds

const val toolGetCurrentDateTime = "get_current_datetime"
const val toolGetInteractingUser = "get_interacting_user"

const val model = OllamaModels.QWEN_2_5_14B

fun main() {
    println("Hello Chat!")

    val client = createKtorClient()


    val tools = listOf<AiTool>(
        AiTool.Function(
            function = AiTool.Function.Data(
                name = toolGetCurrentDateTime,
                description = """
                    Gets the current date and time.
                    
                    Call this tool when you need to provide the current date or time!
                    Call this tool every time you need to provided the current date or time!
                                        
                    Never reuse the result of this tool!
                    Call this tool every time you need to provided the current date or time!
                    
                    Returns a string in the format `yyyy-MM-dd HH:mm:ss`
                """.trimIndent(),
                parameters = AiType.AiObject()
            )
        ),
        AiTool.Function(
            function = AiTool.Function.Data(
                name = toolGetInteractingUser,
                description = """
                    This tool gets information about the user currently interacting with the assistant.
                    
                    Call this tool, when you want to reference the interacting user personally.
                    Call this tool, when the users ask something like `Who am I?` or `What is my name?`.
                    
                    Only call this function when you need to.
                    
                    Returns a string in the format `USER NAME`
                """.trimIndent(),
                parameters = AiType.AiObject()
            )
        )
    )


    val toolDescriptions = tools.joinToString("\n") { it.describe() }

    var conversation = AiConversation.new.add(
        AiConversation.Message.System(
            """
                You are a knowledgeable and helpful assistant. Your primary goal is to answer user questions directly using your built-in knowledge and reasoning. 
                There are available tools that you can call when the query explicitly requires it or when you cannot provide an accurate answer on your own. 
                However, you should only this tools when:
                - The user's request specifically requires information or functionality that the tool provides.
                - You are unable to provide a complete or correct answer directly.

                For all other queries, please provide a direct, complete, and clear answer without using a tool call.
                                
            """.trimIndent()
        )
    )

    while (true) {
        print("Enter a prompt: ")

        when (val prompt = readln()) {
            "/bye" -> {
                break
            }

            "/clear" -> {
                conversation = AiConversation.new
            }

            "/h", "/history" -> {
                conversation.messages.forEach { println(it) }
            }

            else -> {
                conversation = conversation.add(
                    AiConversation.Message.User(prompt)
                )

                runBlocking {
                    conversation = doIt(client, conversation, tools)
                }
            }
        }
    }

    println("Bye!")

    client.close()
}

suspend fun doIt(client: HttpClient, conversation: AiConversation, tools: List<AiTool>): AiConversation {

    val mutable = Mutable(conversation)

    suspend fun call(): List<AiResponse> {
        return client.callLlm(mutable.value, tools)
            .onEach {
//                println(it)
                print(it.message.content ?: "")
            }
            .fold(emptyList()) { acc, msg -> acc + msg }
    }

    var done = false
    var answer: String?

    print("Answer: ")

    while (!done) {
        val parts = call()

        val toolCalls = parts.flatMap { it.message.tool_calls ?: emptyList() }

        if (toolCalls.isNotEmpty()) {
            toolCalls.forEach { toolCall ->
                toolCall.function.let { function ->
                    mutable.modify {
                        // TODO: call the real tool

                        val result = when (function.name) {
                            toolGetCurrentDateTime -> {
                                val format = LocalDateTime.Format {
                                    byUnicodePattern("uuuu-MM-dd HH:mm:ss")
                                }

                                val tz = TimeZone.currentSystemDefault()

                                Clock.System.now().toLocalDateTime(tz).format(format)
                            }

                            toolGetInteractingUser -> {
                                "Karsten Gerber"
                            }

                            else -> "Error! The tool ${function.name} is not available."
                        }

                        println("Calling tool: ${function.name}")

                        it.add(
                            AiConversation.Message.Tool(
                                name = function.name,
                                content = result,
                            )
                        )
                    }
                }
            }
        } else {
            answer = parts
                .mapNotNull { part -> part.message.content?.takeIf { it.isNotBlank() } }
                .joinToString("") { it }

            mutable.modify {
                it.add(AiConversation.Message.Assistant(content = answer))
            }

            done = true
        }
    }

    println()

    return mutable.value
}


suspend fun HttpClient.callLlm(conversation: AiConversation, tools: List<AiTool>): Flow<AiResponse> {

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = true
    }

    // Example GET request
    val request = preparePost("http://127.0.0.1:11434/api/chat") {
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

                    val obj = json.decodeFromString<AiResponse>(buffer)

//                    println("Decoded: $obj")

                    emit(obj)
                }
            }
        }
    }
}

fun createKtorClient(): HttpClient {
    return HttpClient(CIO) {
        engine {

            requestTimeout = 180.seconds.inWholeMilliseconds

            setFollowRedirects(true)
        }
    }
}
