package io.peekandpoke.aktor

import com.typesafe.config.ConfigFactory
import io.peekandpoke.aktor.examples.ExampleBot
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.model.OllamaModels
import kotlinx.coroutines.runBlocking
import java.io.File


fun main() {

    val config = ConfigFactory.parseFile(File("./config/keys.conf"))
    val bot = ExampleBot.createOllamaBot(config = config, model = OllamaModels.QWEN_2_5_3B)

    println("Chatting with model: ${bot.llm.model}")
    println("Available tools:")
    bot.llm.tools.forEach { println("- ${it.tool.describe().split("\n").first()}") }
    println("Have fun!")
    println()

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
                runBlocking {
                    bot.chat(prompt).collect { update ->

                        when (update) {
                            is Llm.Update.Response -> {
                                update.response.message.content?.takeIf { it.isNotBlank() }?.let {
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
                    }
                }
            }
        }
    }

    println()
    println("Bye!")
}

