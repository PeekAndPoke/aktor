package io.peekandpoke.aktor.playgrounds

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.typesafe.config.ConfigFactory
import kotlinx.serialization.json.*
import java.io.File

/**
 * This code snippet demonstrates the use of OpenAI's chat completion capabilities
 * with a focus on integrating function calls into the chat conversation.
 */
suspend fun main() {
    val config = ConfigFactory.parseFile(File("./config/keys.conf"))

    val token = config.getString("keys.OPEN_AI_TOKEN")
    val openAI = OpenAI(token = token, logging = LoggingConfig(logLevel = LogLevel.None))

    println("\n> Create Chat Completion function call...")
    val modelId = ModelId("gpt-4o-mini")
    val chatMessages = mutableListOf(chatMessage {
        role = ChatRole.User
        content = "What's the weather like in San Francisco, Tokyo, and Paris?"
    })
    val request = chatCompletionRequest {
        model = modelId
        messages = chatMessages
        tools {
            function(
                name = "currentWeather",
                description = "Get the current weather in a given location",
            ) {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("location") {
                        put("type", "string")
                        put("description", "The city and state, e.g. San Francisco, CA")
                    }
                    putJsonObject("unit") {
                        put("type", "string")
                        putJsonArray("enum") {
                            add("celsius")
                            add("fahrenheit")
                        }
                    }
                }
                putJsonArray("required") {
                    add("location")
                }
            }
        }
        toolChoice = ToolChoice.Auto // or ToolChoice.function("currentWeather")
    }

    val response = openAI.chatCompletion(request)
    val message = response.choices.first().message

    chatMessages.append(message)

    println("===================================================================")
    println(chatMessages.joinToString("\n\n") { it.toString() })

    for (toolCall in message.toolCalls.orEmpty()) {
        require(toolCall is ToolCall.Function) { "Tool call is not a function" }
        val functionResponse = toolCall.execute()
        chatMessages.append(toolCall, functionResponse)
    }

    println("===================================================================")
    println(chatMessages.joinToString("\n\n") { it.toString() })

    val secondResponse = openAI.chatCompletion(
        request = ChatCompletionRequest(model = modelId, messages = chatMessages)
    )

    print(secondResponse.choices.first().message.content.orEmpty())
}

/**
 * A map that associates function names with their corresponding functions.
 */
private val availableFunctions = mapOf("currentWeather" to ::callCurrentWeather)

/**
 * Example of a fake function for retrieving weather information based on location and temperature unit.
 * In a production scenario, this function could be replaced with an actual backend or external API call.
 */
private fun callCurrentWeather(args: JsonObject): String {
    val location = args.getValue("location").jsonPrimitive.content

    return when  {
        location.contains("San Francisco") -> """"{"location": "San Francisco", "temperature": "72", "unit": "fahrenheit"}"""
        location.contains("Tokyo") -> """{"location": "Tokyo", "temperature": "10", "unit": "celsius"}"""
        location.contains("Paris") -> """{"location": "Paris", "temperature": "22", "unit": "celsius"}"""
        else -> """{"location": "$location", "temperature": "unknown", "unit": "unknown"}"""
    }
}

/**
 * Executes a function call and returns its result.
 */
private fun ToolCall.Function.execute(): String {
    val functionToCall = availableFunctions[function.name] ?: error("Function ${function.name} not found")
    val functionArgs = function.argumentsAsJson()
    return functionToCall(functionArgs)
}

/**
 * Appends a chat message to a list of chat messages.
 */
private fun MutableList<ChatMessage>.append(message: ChatMessage) {
    add(
        ChatMessage(
            role = message.role,
            content = message.content.orEmpty(),
            toolCalls = message.toolCalls,
            toolCallId = message.toolCallId,
        )
    )
}

/**
 * Appends a function call and response to a list of chat messages.
 */
private fun MutableList<ChatMessage>.append(toolCall: ToolCall.Function, functionResponse: String) {
    val message = ChatMessage(
        role = ChatRole.Tool,
        toolCallId = toolCall.id,
        name = toolCall.function.name,
        content = functionResponse
    )
    add(message)
}
