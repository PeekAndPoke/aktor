package io.peekandpoke.aktor.cli

import com.github.ajalt.clikt.core.CliktCommand
import de.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.aktor.KeysConfig
import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.examples.ExampleBots
import io.peekandpoke.aktor.llm.ChatBot
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llms.mcp.client.McpClient
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
        val tools = kontainer.get(ExampleBots::class).builtInTools.plus(mcpTools)

        var conversation = AiConversation(
            ownerId = "tmp",
            messages = listOf(ChatBot.defaultSystemMessage),
            tools = tools.map { it.toToolRef() },
        )

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
        )

        println("Chatting with model: ${bot.llm.model}")

        println("Available tools:")
        conversation.tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description.split("\n").first()}")

            tool.parameters.forEach { name, desc ->
                println("  param: $name $desc")
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
                    conversation = conversation.copy(messages = emptyList())
                }

                "/h", "/history" -> {
                    conversation.messages.forEach { println(it) }
                }

                else -> {
                    bot.chat(conversation, prompt, tools).collect { update ->
                        conversation = update.conversation

                        when (update) {
                            is Llm.Update.Start -> Unit
                            is Llm.Update.Response -> update.content?.let { print(it) }
                            is Llm.Update.Stop -> println()
                            is Llm.Update.Info -> println(update.message)
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
