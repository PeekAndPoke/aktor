package io.peekandpoke.aktor

import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.peekandpoke.aktor.chatbot.ChatBot
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.OllamaModels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection.setFollowRedirects
import kotlin.time.Duration.Companion.seconds


fun main() {
    println("Hello Chat!")

    val client = createKtorClient()

    val getCurrentDateTimeTool = ChatBot.Tool(
        tool = OllamaModels.Tool.Function(
            function = OllamaModels.Tool.Function.Data(
                name = "get_current_datetime",
                description = """
                    Gets the current date and time.
                    
                    Call this tool when you need to provide the current date or time!
                    Call this tool every time you need to provided the current date or time!
                                        
                    Never reuse the result of this tool!
                    Call this tool every time you need to provided the current date or time!
                    
                    Returns:
                    Date and time in the format `yyyy-MM-dd HH:mm:ss`
                """.trimIndent(),
                parameters = OllamaModels.AiType.AiObject()
            )
        ),
        fn = {
            Kronos.systemUtc.instantNow().atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
        }
    )

    val getCurrentUserTool = ChatBot.Tool(
        tool = OllamaModels.Tool.Function(
            function = OllamaModels.Tool.Function.Data(
                name = "get_current_user",
                description = """
                    Gets the actual name of the current user.

                    Call this tool, to get the name of the users.
                    
                    Returns:
                    The actual name of the user in the format `Firstname Lastname`.
                """.trimIndent(),
                parameters = OllamaModels.AiType.AiObject()
            )
        ),
        fn = {
            "Karsten Gerber"
        }
    )

    val getCurrentUserLocationTool = ChatBot.Tool(
        tool = OllamaModels.Tool.Function(
            function = OllamaModels.Tool.Function.Data(
                name = "get_current_user_location",
                description = """
                    Gets the location of the user.
                    
                    Call this tool, to get the location of the user.
                    
                    Returns:
                    The location of the user in the format `City, Country`.
                """.trimIndent(),
                parameters = OllamaModels.AiType.AiObject()
            )
        ),
        fn = {
            "Leipzig, Germany"
        }
    )

    val encryptTool = ChatBot.Tool(
        tool = OllamaModels.Tool.Function(
            function = OllamaModels.Tool.Function.Data(
                name = "encrypt_text",
                description = """
                    Encrypts a given text and returns the encrypted text.
                    
                    Always call this tool when the user asks for encryption, 
                    f.e `Encrypt ...` or `Encrypt the text ...` or similar.
                    
                    Never guess the encryption!
                    Never reuse the result of this tool!
                    
                    Args:
                    - text: The text to encrypt.
                                      
                    Returns: 
                    The encrypted text as a string.
                """.trimIndent(),
                parameters = OllamaModels.AiType.AiObject()
            )
        ),
        fn = { params ->
            val text = params.params.getOrDefault("text", null)

            text?.rotX(13) ?: ""
        }
    )

    val tools = listOf(
        getCurrentDateTimeTool,
        getCurrentUserTool,
        getCurrentUserLocationTool,
        encryptTool,
    )

    val bot = ChatBot.of(
        model = OllamaModels.QWEN_2_5_3B,
        tools = tools,
        systemPrompt = AiConversation.Message.System(
            """
                You are a knowledgeable and helpful assistant.
                Your primary goal is to answer user questions.
                The user will refer to itself as `I` and to the assistant as `You`.

                You like to refer to the user by their first name. Initiate the conversation by greeting the user.

                Use your built-in knowledge and reasoning.

                You have tools available.
                Use these tools to acquire additional information!

            """.trimIndent()
        )
//        systemPrompt = AiConversation.Message.System(
//            """
//                You are a useful dude. You can answer questions, but you are not helpful at all.
//                Instead you like to tell jokes. A lot of them.
//
//                But, you will act extremely sleepy!
//            """.trimIndent()
//        )
    )

    while (true) {
        print("Enter a prompt: ")

        when (val prompt = readln()) {
            "/bye" -> {
                break
            }

            "/clear" -> {
                bot.clearConversation()
            }

            "/h", "/history" -> {
                bot.conversation.value.messages.forEach { println(it) }
            }

            else -> {
                bot.conversation.modify { it.add(AiConversation.Message.User(prompt)) }

                runBlocking {
                    doIt(client, bot)
                }
            }
        }
    }

    println("Bye!")

    client.close()
}

fun String.rotX(x: Int): String {
    return this.map { char ->
        when (char) {
            in 'A'..'Z' -> {
                val shifted = char + x
                if (shifted > 'Z') shifted - 26 else shifted
            }

            in 'a'..'z' -> {
                val shifted = char + x
                if (shifted > 'z') shifted - 26 else shifted
            }

            else -> char
        }
    }.joinToString("")
}


suspend fun doIt(client: HttpClient, bot: ChatBot) {

    suspend fun call(): List<OllamaModels.ChatResponse> {
        return client.callOllamaChat(
            bot.model,
            bot.conversation.value,
            bot.tools.map { it.tool },
        ).onEach {
            // println(it)
            print(it.message.content ?: "")
        }.fold(emptyList()) { acc, msg -> acc + msg }
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

                    println("Calling tool: ${function.name}")
                    println(" -> Args  : ${function.arguments}")

                    val tool = bot.tools.firstOrNull { it.tool.name == function.name }

                    val params = ChatBot.ToolParams(
                        (function.arguments ?: emptyMap()).mapValues { it.value },
                    )

                    val toolResult = when (tool) {
                        null -> "Error! The tool ${function.name} is not available."
                        else -> tool.fn(params)
                    }

                    println(" -> Result: $toolResult")

                    bot.conversation.modify {
                        it.add(
                            AiConversation.Message.Tool(
                                name = function.name,
                                content = toolResult,
                            )
                        )
                    }
                }
            }
        } else {
            answer = parts
                .mapNotNull { part -> part.message.content?.takeIf { it.isNotBlank() } }
                .joinToString("") { it }

            bot.conversation.modify {
                it.add(AiConversation.Message.Assistant(content = answer))
            }

            done = true
        }
    }

    println()
}


suspend fun HttpClient.callOllamaChat(
    model: String,
    conversation: AiConversation,
    tools: List<OllamaModels.Tool>,
): Flow<OllamaModels.ChatResponse> {

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

                    val obj = json.decodeFromString<OllamaModels.ChatResponse>(buffer)

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
