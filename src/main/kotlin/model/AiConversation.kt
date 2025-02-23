package io.peekandpoke.aktor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

data class AiConversation(
    val messages: List<Message>,
) {
    companion object {
        val new = AiConversation(
            messages = emptyList(),
        )
    }

    @Serializable
    sealed interface Message {
        @Serializable
        @SerialName("system")
        data class System(
            val content: String,
        ) : Message

        @Serializable
        @SerialName("assistant")
        data class Assistant(
            val content: String?,
            val toolCalls: List<ToolCall>? = null,
        ): Message

        @Serializable
        @SerialName("user")
        data class User(
            val content: String,
        ): Message

        @Serializable
        @SerialName("tool")
        data class Tool(
            val content: String,
            val toolCall: ToolCall,
        ): Message

        @Serializable
        data class ToolCall(
            val id: String,
            val name: String,
            val args: Args,
        ) {
            @Serializable
            data class Args(
                val params: JsonObject,
            ) {
                companion object {
                    fun ofMap(params: Map<String, Any?>?): Args {
                        val obj = buildJsonObject {
                            params?.forEach { (key, value) ->
                                when (value) {
                                    null -> put(key, JsonNull)
                                    is Boolean -> put(key, value)
                                    is Number -> put(key, value)
                                    is String -> put(key, value)
                                    is Char -> put(key, value.toString())
                                }
                            }
                        }

                        return Args(obj)
                    }
                }

                fun toMap() = params.mapValues { (_, value) ->
                    when (value) {
                        JsonNull -> null
                        is JsonPrimitive -> value.contentOrNull
                        else -> null
                    }
                }

                fun print(): String = Json.encodeToString(params)

                fun getString(name: String): String? = params[name]?.jsonPrimitive?.content

                fun getDouble(name: String): Double? = getString(name)?.toDoubleOrNull()

                fun getInt(name: String): Int? = getString(name)?.toIntOrNull()

                fun getBoolean(name: String): Boolean? = getString(name)?.toBooleanStrictOrNull()
            }
        }
    }

    fun add(message: Message): AiConversation = copy(
        messages = messages + message
    )
}
