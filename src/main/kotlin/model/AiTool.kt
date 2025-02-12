package io.peekandpoke.aktor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
sealed interface AiTool {

    // https://github.com/ollama/ollama/blob/main/docs/api.md#chat-request-with-tools

    @Serializable
    @SerialName("function")
    data class Function(
        val function: Data,
    ): AiTool {
        @Serializable
        data class Data(
            val name: String,
            val description: String,
            val parameters: AiType,
        )

        override fun describe(): String {
            return "${function.name}() - ${function.description}"
        }
    }

    fun describe(): String
}

@Serializable
@JsonClassDiscriminator("type")
sealed interface AiType {

    @Serializable
    @SerialName("object")
    data class AiObject(
        val properties: List<AiType>? = null,
        val required: List<String>? = null,
    ) : AiType

    @Serializable
    @SerialName("string")
    data class AiString(val description: String) : AiType
}
