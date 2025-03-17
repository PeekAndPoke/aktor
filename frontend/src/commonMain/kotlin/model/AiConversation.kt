package io.peekandpoke.aktor.model

import com.benasher44.uuid.uuid4
import de.peekandpoke.ultra.common.replaceFirstByOrAdd
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
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
            override val uuid: String = uuid4().toString(),
            val content: String,
        ) : Message

        @Serializable
        @SerialName("assistant")
        data class Assistant(
            override val uuid: String = uuid4().toString(),
            val content: String? = null,
            val toolCalls: List<ToolCall>? = null,
        ) : Message {
            fun appendContent(content: String?) = copy(content = this.content.orEmpty() + content.orEmpty())
        }

        @Serializable
        @SerialName("user")
        data class User(
            override val uuid: String = uuid4().toString(),
            val content: String,
        ) : Message

        @Serializable
        @SerialName("tool")
        data class Tool(
            override val uuid: String = uuid4().toString(),
            val content: String,
            val toolCall: ToolCall,
        ) : Message

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
                    val empty = ofMap(emptyMap())

                    fun tryParseOrEmpty(json: String?): Args = tryParse(json) ?: empty

                    fun tryParse(json: String?): Args? = try {
                        when (json) {
                            null -> empty
                            else -> Args(Json.parseToJsonElement(json).jsonObject)
                        }
                    } catch (_: Exception) {
                        null
                    }

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

                fun print(): String = Json.Default.encodeToString(params)

                fun getString(name: String): String? = params[name]?.jsonPrimitive?.content

                fun getDouble(name: String): Double? = getString(name)?.toDoubleOrNull()

                fun getInt(name: String): Int? = getString(name)?.toIntOrNull()

                fun getBoolean(name: String): Boolean? = getString(name)?.toBooleanStrictOrNull()
            }
        }

        val uuid: String
    }

    fun addOrUpdate(messages: List<Message>): AiConversation {
        return messages.fold(this) { acc, message -> acc.addOrUpdate(message) }
    }

    fun addOrUpdate(message: Message): AiConversation {

        val newMessages = messages.replaceFirstByOrAdd(message) { it.uuid == message.uuid }

        return copy(messages = newMessages)
    }
}
