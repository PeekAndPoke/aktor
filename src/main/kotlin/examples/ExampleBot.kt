package io.peekandpoke.aktor.examples

import com.typesafe.config.Config
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import io.peekandpoke.aktor.chatbot.ChatBot
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llm.OllamaLlm
import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.OllamaModels
import io.peekandpoke.aktor.tools.IpApiCom
import io.peekandpoke.aktor.tools.IpInfoIo
import io.peekandpoke.aktor.tools.OpenMeteoCom

object ExampleBot {
    fun createOllamaBot(
        config: Config,
        model: String,
    ): ChatBot {
        val getCurrentDateTimeTool = Llm.Tool(
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

        val getCurrentUserTool = Llm.Tool(
            tool = OllamaModels.Tool.Function(
                function = OllamaModels.Tool.Function.Data(
                    name = "get_current_user",
                    description = """
                    Gets the actual name of the current user.
                    
                    Call this tool, to get the actual name of the current user.
                    
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

        val getCurrentUserLocationTool = Llm.Tool(
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

        val encryptTool = Llm.Tool(
            tool = OllamaModels.Tool.Function(
                function = OllamaModels.Tool.Function.Data(
                    name = "encrypt_text",
                    description = """
                    Encrypts the given text.
                    
                    Always call this tool when the user asks for encryption, 
                    f.e `Encrypt ...` or `Encrypt the text ...` or similar.
                    
                    Never guess the encryption!
                    Never reuse the result of this tool!
                    
                    Returns: 
                    The encrypted text as a string.
                """.trimIndent(),
                    parameters = OllamaModels.AiType.AiObject(
                        properties = mapOf(
                            "text" to OllamaModels.AiType.AiString("The text to encrypt.")
                        ),
                        required = listOf("text"),
                    ),
                ),
            ),
            fn = { params ->
                val text = params.params.getOrDefault("text", null)

                text?.rotX(13) ?: ""
            }
        )

        val tools = listOf(
            // Api tools
//            IpApiCom.tool(),
            IpInfoIo.tool(config.getString("keys.IP_INFO_TOKEN")),
            OpenMeteoCom.tool(),
            // sample tools
            getCurrentDateTimeTool,
            getCurrentUserTool,
//            getCurrentUserLocationTool,
            encryptTool,
        )

        val llm = OllamaLlm(model = model, tools = tools)

        val bot = ChatBot.of(
            llm = llm,
            systemPrompt = AiConversation.Message.System(
                """
                You are a knowledgeable and helpful assistant.
                Your primary goal is to answer user questions.

                You also have tools available.
                Use these tools to acquire additional information!

                The current user might refer to itself as `I`, `me` or `my`.
                The user might refer to the assistant as `You`.
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

        return bot
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
}
