package io.peekandpoke.aktor.examples

import io.peekandpoke.aktor.llm.ChatBot
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llm.ollama.OllamaLlm
import io.peekandpoke.aktor.llm.openai.OpenAiLlm
import io.peekandpoke.aktor.llm.tools.*

class ExampleBots(
    exchangeRateApiCom: ExchangeRateApiCom,
    ipInfoIo: IpInfoIo,
    mathParserTool: MathParserTool,
    openMeteoCom: OpenMeteoCom,
    rausgegangenDe: RausgegangenDe,
) {

    val builtInTools = emptyList<Llm.Tool>()
        .plus(exchangeRateApiCom.asLlmTool())
        .plus(ipInfoIo.asLlmTool())
        .plus(mathParserTool.asLlmTool())
        .plus(openMeteoCom.asLlmTools())
        .plus(rausgegangenDe.asLlmTool())
        // Example tools
        .plus(ExampleLlmUtils.getCurrentDateTimeTool)
        .plus(ExampleLlmUtils.getUsersNameTool)
        .plus(ExampleLlmUtils.encryptTool)

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
