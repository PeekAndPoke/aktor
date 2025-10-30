package io.peekandpoke.aktor.backend.aiconversation

import com.benasher44.uuid.uuid4
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.replaceFirstByOrAdd
import de.peekandpoke.ultra.vault.Vault
import de.peekandpoke.ultra.vault.hooks.Timestamped
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel
import io.peekandpoke.aktor.utils.toJsonObject
import io.peekandpoke.aktor.utils.unwrap
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Vault
data class AiConversation(
    val ownerId: String,
    val messages: List<Message>,
    val tools: List<AiConversationModel.ToolRef> = emptyList(),
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {

    companion object {
        fun new(ownerId: String) = AiConversation(
            ownerId = ownerId,
            tools = emptyList(),
            messages = emptyList(),
        )
    }

    sealed interface Message {
        @SerialName("system")
        data class System(
            override val uuid: String = uuid4().toString(),
            val content: String,
        ) : Message {
            override fun asApiModel() = AiConversationModel.Message.System(
                uuid = uuid,
                content = content,
            )
        }

        @SerialName("assistant")
        data class Assistant(
            override val uuid: String = uuid4().toString(),
            val content: String? = null,
            val toolCalls: List<ToolCall>? = null,
            val rawResponse: JsonElement? = null,
        ) : Message {
            fun appendContent(content: String?) =
                copy(content = this.content.orEmpty() + content.orEmpty())

            override fun asApiModel() = AiConversationModel.Message.Assistant(
                uuid = uuid,
                content = content,
                toolCalls = toolCalls?.map { it.asApiModel() },
                rawResponse = rawResponse,
            )
        }

        @SerialName("user")
        data class User(
            override val uuid: String = uuid4().toString(),
            val content: String,
        ) : Message {
            override fun asApiModel() = AiConversationModel.Message.User(
                uuid = uuid,
                content = content,
            )
        }

        @SerialName("tool")
        data class Tool(
            override val uuid: String = uuid4().toString(),
            val content: String,
            val toolCall: ToolCall,
        ) : Message {
            override fun asApiModel() = AiConversationModel.Message.Tool(
                uuid = uuid,
                content = content,
                toolCall = toolCall.asApiModel(),
            )
        }

        data class ToolCall(
            val id: String,
            val name: String,
            val args: Args,
        ) {
            data class Args(
                val params: Map<String, Any?>,
            ) {
                companion object {
                    val empty = Args(emptyMap())

                    fun tryParseOrEmpty(json: String?): Args = tryParse(json) ?: empty

                    fun tryParse(json: String?): Args? = try {
                        when (json) {
                            null -> empty
                            else -> Args(Json.parseToJsonElement(json).jsonObject.unwrap())
                        }
                    } catch (_: Exception) {
//                        e.printStackTrace()
                        null
                    }

                    fun ofMap(params: Map<String, Any?>?): Args {
                        return Args(
                            params ?: emptyMap()
                        )
                    }
                }

                val json: JsonObject by lazy { params.toJsonObject() }

                fun print(): String = Json.encodeToString(params.toJsonObject())

                fun getString(name: String): String? = params[name]?.toString()

                fun getDouble(name: String): Double? = getString(name)?.toDoubleOrNull()

                fun getInt(name: String): Int? = getString(name)?.toIntOrNull()

                fun getBoolean(name: String): Boolean? = getString(name)?.toBooleanStrictOrNull()
            }

            fun asApiModel() = AiConversationModel.Message.ToolCall(
                id = id,
                name = name,
                args = AiConversationModel.Message.ToolCall.Args(
                    params = args.params.mapValues { it.value.toString() },
                ),
            )
        }

        val uuid: String

        fun asApiModel(): AiConversationModel.Message
    }

    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)

    fun addOrUpdate(messages: List<Message>): AiConversation {
        return messages.fold(this) { acc, message -> acc.addOrUpdate(message) }
    }

    fun addOrUpdate(message: Message): AiConversation {

        val newMessages = messages.replaceFirstByOrAdd(message) { it.uuid == message.uuid }

        return copy(messages = newMessages)
    }
}
