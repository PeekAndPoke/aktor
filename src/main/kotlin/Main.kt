package io.peekandpoke.aktor

import com.typesafe.config.ConfigFactory
import io.peekandpoke.aktor.examples.ExampleBot
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llm.ollama.OllamaModels
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File


fun main() {

    val config = ConfigFactory.parseFile(File("./config/keys.conf"))

    val bot = ExampleBot.createOllamaBot(
        config = config,
        model = OllamaModels.LLAMA_3_2_3B,
    )

//    val bot = ExampleBot.createOpenAiBot(
//        config = config,
//        model = "gpt-4o-mini"
//    )

    println("Chatting with model: ${bot.llm.model}")
    println("Available tools:")
    bot.llm.tools.forEach { println("- ${it.describe().split("\n").first()}") }
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
                                update.content?.takeIf { it.isNotBlank() }?.let {
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

