package io.peekandpoke.aktor.llms.ollama

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

@Suppress("PropertyName")
object OllamaModels {
    const val DEEPSEEK_R1_7B = "deepseek-r1:7B"

    const val GEMMA_3_1B = "gemma3:1b"
    const val GEMMA_3_4B = "gemma3:4b"
    const val GEMMA_3_12B = "gemma3:12b"
    const val GEMMA_3_27B = "gemma3:27b"

    const val LLAMA_3_2_1B = "llama3.2:1b"
    const val LLAMA_3_2_3B = "llama3.2:3b"

    const val MISTRAL_SMALL_22B = "mistral-small:22b"
    const val MISTRAL_SMALL_24B = "mistral-small:24b"

    const val QWEN_2_5_3B = "qwen2.5:3b"
    const val QWEN_2_5_7B = "qwen2.5:7b"
    const val QWEN_2_5_14B = "qwen2.5:14b"

    const val SMOLLM2_135M = "smollm2:135m"
    const val SMOLLM2_360M = "smollm2:360m"
    const val SMOLLM2_1_7B = "smollm2:1.7b"

    // https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-chat-completion
    @Serializable
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val stream: Boolean = false,
        val keep_alive: String = "10m",
        val options: Options = Options.DEFAULT,
        val tools: List<Tool>? = null,
    ) {
        @Serializable
        data class Options(
            val temperature: Double? = null,
            val seed: Long? = null,
        ) {
            companion object {
                val DEFAULT = Options()
            }
        }
    }

    @Serializable
    @JsonClassDiscriminator("role")
    sealed interface Message {
        @Serializable
        @SerialName("system")
        data class System(
            val content: String,
        ) : Message

        @Serializable
        @SerialName("assistant")
        data class Assistant(
            val content: String,
            val tool_calls: List<ToolCall>? = null,
        ) : Message

        @Serializable
        @SerialName("user")
        data class User(
            val content: String,
        ) : Message

        @Serializable
        @SerialName("tool")
        data class Tool(
            val name: String,
            val content: String,
            val tool_calls: List<ToolCall>? = null,
        ) : Message

        @Serializable
        data class ToolCall(
            val function: Function,
        ) {
            @Serializable
            data class Function(
                val name: String,
                val arguments: JsonObject? = null,
            )
        }
    }


    @Serializable
    @JsonClassDiscriminator("type")
    sealed interface Tool {

        // https://github.com/ollama/ollama/blob/main/docs/api.md#chat-request-with-tools

        @Serializable
        @SerialName("function")
        data class Function(
            val function: Data,
        ) : Tool {
            @Serializable
            data class Data(
                val name: String,
                val description: String,
                val parameters: Type,
            )

            override val name: String = function.name

            override fun describe(): String {
                return "${function.name}() - ${function.description}"
            }
        }

        val name: String

        fun describe(): String
    }

    @Suppress("PropertyName")
    @Serializable
    data class ChatResponse(
        val model: String,
        val created_at: String,
        val message: Message,
        val done: Boolean,
        val done_reason: String? = null,
        val total_duration: Long? = null,
        val load_duration: Long? = null,
        val prompt_eval_count: Long? = null,
        val prompt_eval_duration: Long? = null,
        val eval_count: Long? = null,
        val eval_duration: Long? = null,
    ) {
        @Serializable
        data class Message(
            val role: String,
            val content: String? = null,
            val tool_calls: List<ToolCall>? = null,
        )

        @Serializable
        data class ToolCall(
            val function: Function,
        ) {
            @Serializable
            data class Function(
                val name: String,
                val arguments: Map<String, String>? = null,
            )
        }
    }

    @Serializable
    @JsonClassDiscriminator("type")
    sealed interface Type

    @Serializable
    @SerialName("object")
    data class ObjectType(
        val properties: Map<String, Type>? = null,
        val required: List<String>? = null,
    ) : Type

    @Serializable
    @SerialName("string")
    data class StringType(val description: String) : Type

    @Serializable
    @SerialName("number")
    data class NumberType(val description: String) : Type

    @Serializable
    @SerialName("integer")
    data class IntegerType(val description: String) : Type

    @Serializable
    @SerialName("boolean")
    data class BooleanType(val description: String) : Type
}
