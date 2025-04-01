package io.peekandpoke.aktor.examples

import io.peekandpoke.aktor.llms.ChatBot
import io.peekandpoke.aktor.llms.Llm
import io.peekandpoke.aktor.llms.ollama.OllamaLlm
import io.peekandpoke.aktor.llms.openai.OpenAiLlm
import io.peekandpoke.aktor.llms.tools.*

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
    ): ChatBot {
        val llm = OllamaLlm(model = model)
        val bot = ChatBot(llm = llm, streaming = streaming)

        return bot
    }

    fun createOpenAiBot(
        apiKey: String,
        model: String,
        streaming: Boolean,
    ): ChatBot {
        val llm = OpenAiLlm(model = model, authToken = apiKey)
        val bot = ChatBot(llm = llm, streaming = streaming)

        return bot
    }
}
