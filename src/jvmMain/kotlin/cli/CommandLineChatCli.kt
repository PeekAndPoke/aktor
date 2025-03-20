package io.peekandpoke.aktor.cli

import com.github.ajalt.clikt.core.CliktCommand
import de.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.aktor.KeysConfig
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llm.mcp.client.McpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class CommandLineChatCli(
    private val kontainer: Kontainer,
) : CliktCommand(name = "chat") {

    override fun run() {
        println("Running chat")

        runBlocking {
            val deferred = async { runChat() }
            deferred.await()
        }
    }

    private suspend fun runChat() {
        val keys = kontainer.get(KeysConfig::class)
        val exampleBots = kontainer.get(ExampleBots::class)

        val mcpClient = McpClient(
            name = "Play",
            version = "1.0.0",
            toolNamespace = "playground",
        ).connect()

        val mcpTools = mcpClient.listToolsBound() ?: emptyList()

//    val bot = ExampleBot.createOllamaBot(
//        config = config,
//        model = OllamaModels.QWEN_2_5_14B,
//        streaming = false,
//    )
//
        val bot = exampleBots.createOpenAiBot(
            apiKey = keys.config.getString("OPENAI_API_KEY"),
            model = "gpt-4o-mini",
            streaming = true,
            tools = mcpTools,
        )

        println("Chatting with model: ${bot.llm.model}")

        println("Available tools:")
        bot.llm.tools.forEach { tool ->
            println("- ${tool.describe().split("\n").first()}")

            when (tool) {
                is Llm.Tool.Function -> tool.parameters.forEach { p ->
                    println("  param: ${p.name} ${p::class.simpleName} (${p.description})")
                }
            }
        }

        println("Have fun!")
        println()

        while (true) {
            print("You: ")

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
                    bot.chat(prompt).collect { update ->

                        when (update) {
                            is Llm.Update.Response -> {
                                update.content?.let {
                                    print(it)
                                }
                            }

                            is Llm.Update.Stop -> {
                                println()
                            }

                            is Llm.Update.Info -> {
                                println(update.message)
                            }
                        }

                        System.out.flush()
                    }
                }
            }
        }

        println()
        println("Bye!")
    }
}
