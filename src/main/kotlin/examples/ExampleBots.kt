package io.peekandpoke.aktor.examples

import de.peekandpoke.ultra.common.datetime.Kronos
import io.peekandpoke.aktor.chatbot.ChatBot
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llm.ollama.OllamaLlm
import io.peekandpoke.aktor.llm.openai.OpenAiLlm
import io.peekandpoke.aktor.tools.ExchangeRateApiCom
import io.peekandpoke.aktor.tools.IpInfoIo
import io.peekandpoke.aktor.tools.OpenMeteoCom
import io.peekandpoke.aktor.tools.RausgegangenDe

class ExampleBots(
    exchangeRateApiCom: ExchangeRateApiCom,
    ipInfoIo: IpInfoIo,
    openMeteoCom: OpenMeteoCom,
    rausgegangenDe: RausgegangenDe,
) {
    val getCurrentDateTimeTool = Llm.Tool.Function(
        name = "get_current_datetime",
        description = """
            Gets the current date and time.
                            
            Returns:
                Date and time in the format `EEEE yyyy-MM-dd HH:mm:ss` or `weekday date time`
        """.trimIndent(),
        parameters = emptyList(),
        fn = {
            Kronos.systemUtc.instantNow().atSystemDefaultZone().format("EEEE yyyy-MM-dd HH:mm:ss")
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

    val builtInTools = emptyList<Llm.Tool>()
        .plus(exchangeRateApiCom.asLlmTool())
        .plus(ipInfoIo.asLlmTool())
        .plus(openMeteoCom.asLlmTools())
        .plus(rausgegangenDe.asLlmTool())
        // Example tools
        .plus(getCurrentDateTimeTool)
        .plus(getUsersNameTool)
        .plus(encryptTool)

    fun createOllamaBot(
        model: String,
        streaming: Boolean,
        tools: List<Llm.Tool> = emptyList(),
    ): ChatBot {

        val allTools = builtInTools.plus(tools)

        val llm = OllamaLlm(model = model, tools = allTools)

        val bot = ChatBot.of(llm = llm, streaming = streaming)

        return bot
    }

    fun createOpenAiBot(
        apiKey: String,
        model: String,
        streaming: Boolean,
        tools: List<Llm.Tool> = emptyList(),
    ): ChatBot {
        val allTools = builtInTools.plus(tools)

        val llm = OpenAiLlm(model = model, tools = allTools, authToken = apiKey)
        val bot = ChatBot.of(llm = llm, streaming = streaming)

        return bot
    }

}
