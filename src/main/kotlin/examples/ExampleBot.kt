package io.peekandpoke.aktor.examples

import com.typesafe.config.Config
import de.peekandpoke.ultra.common.datetime.Kronos
import io.peekandpoke.aktor.chatbot.ChatBot
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llm.ollama.OllamaLlm
import io.peekandpoke.aktor.llm.openai.OpenAiLlm
import io.peekandpoke.aktor.tools.ExchangeRateApiCom
import io.peekandpoke.aktor.tools.IpInfoIo
import io.peekandpoke.aktor.tools.OpenMeteoCom

object ExampleBot {
    val getCurrentDateTimeTool = Llm.Tool.Function(
        name = "get_current_datetime",
        description = """
            Gets the current date and time.
                            
            Returns:
            Date and time in the format `yyyy-MM-dd HH:mm:ss`
        """.trimIndent(),
        parameters = emptyList(),
        fn = {
            Kronos.systemUtc.instantNow().atSystemDefaultZone().format("yyyy-MM-dd HH:mm:ss")
        }
    )

    val getUsersNameTool = Llm.Tool.Function(
        name = "get_users_name",
        description = """
            Gets the of the Human interacting with the assistant.
            
            This is NOT the name of the bot.
            
            Returns:
            the users name like `Firstname Lastname`
        """.trimIndent(),
        parameters = emptyList(),
        fn = {
            "Karsten Gerber"
        }
    )

    val getCurrentUserLocationTool = Llm.Tool.Function(
        name = "get_current_user_location",
        description = """
            Gets the location of the user.
            
            Returns:
            The location of the user in the format `City, Country`.
        """.trimIndent(),
        parameters = emptyList(),
        fn = {
            "Leipzig, Germany"
        }
    )

    val encryptTool = Llm.Tool.Function(
        name = "encrypt_text",
        description = """
            Encrypts the given text.
            
            Always use this tool for encryption. Never guess the encryption!
            Never remember the result!
            
            Returns: 
            The encrypted text as a string.
        """.trimIndent(),
        parameters = listOf(
            Llm.Tool.StringParam(name = "text", description = "The text to encrypt.", required = true),
        ),
        fn = { params ->
            val text = params.getString("text") ?: error("Missing parameter 'text'")

            text.rotX(13)
        }
    )

    fun createOllamaBot(config: Config, model: String, streaming: Boolean): ChatBot {

        val tools = listOf(
            // Api tools
//            IpApiCom.tool(),
            IpInfoIo.tool(config.getString("keys.IP_INFO_TOKEN")),
            OpenMeteoCom.tool(),
            // sample tools
            getCurrentDateTimeTool,
            getUsersNameTool,
//            getCurrentUserLocationTool,
            encryptTool,
        )

        val llm = OllamaLlm(model = model, tools = tools)

        val bot = ChatBot.of(llm = llm, streaming = streaming)

        return bot
    }

    fun createOpenAiBot(config: Config, model: String, streaming: Boolean): ChatBot {
        val tools = listOf(
            IpInfoIo.tool(config.getString("keys.IP_INFO_TOKEN")),
            OpenMeteoCom.tool(),
            ExchangeRateApiCom.tool(),
            //
            getCurrentDateTimeTool,
            getUsersNameTool,
            encryptTool,
        )

        val token = config.getString("keys.OPEN_AI_TOKEN")

        val llm = OpenAiLlm(model = model, tools = tools, authToken = token)
        val bot = ChatBot.of(llm = llm, streaming = streaming)

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
