package io.peekandpoke.aktor.shared.aiconversation.model

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class AiConversationModel(
    val id: String,
    val ownerId: String,
    val messages: List<Message>,
    val tools: List<ToolRef>,
    val createdAt: MpInstant,
    val updatedAt: MpInstant,
) {
    data class MessageStats(
        val numTotal: Int,
        val numSystem: Int,
        val numAssistant: Int,
        val numUser: Int,
        val numTool: Int,
    )

    @Serializable
    data class ToolRef(
        val name: String,
        val description: String,
        val parameters: Map<String, String>,
    )

    @Serializable
    sealed interface Message {
        @Serializable
        @SerialName("system")
        data class System(
            override val uuid: String,
            val content: String,
        ) : Message

        @Serializable
        @SerialName("assistant")
        data class Assistant(
            override val uuid: String,
            val content: String?,
            val toolCalls: List<ToolCall>?,
            val rawResponse: JsonElement? = null,
        ) : Message

        @Serializable
        @SerialName("user")
        data class User(
            override val uuid: String,
            val content: String,
        ) : Message

        @Serializable
        @SerialName("tool")
        data class Tool(
            override val uuid: String,
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
                val params: Map<String, String?>,
            ) {
                fun print(): String = Json.Default.encodeToString(params)

                fun getString(name: String): String? = params[name]

                fun getDouble(name: String): Double? = getString(name)?.toDoubleOrNull()

                fun getInt(name: String): Int? = getString(name)?.toIntOrNull()

                fun getBoolean(name: String): Boolean? = getString(name)?.toBooleanStrictOrNull()
            }
        }

        val uuid: String
    }

    val stats by lazy {
        MessageStats(
            numTotal = messages.size,
            numSystem = messages
                .filterIsInstance<Message.System>()
                .count(),
            numAssistant = messages
                .filterIsInstance<Message.Assistant>()
                .count { it.content?.isNotBlank() == true },
            numUser = messages
                .filterIsInstance<Message.User>()
                .count(),
            numTool = messages
                .filterIsInstance<Message.Tool>()
                .count(),
        )
    }
}
