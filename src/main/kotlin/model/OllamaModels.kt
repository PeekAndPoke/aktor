package io.peekandpoke.aktor.model

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
object OllamaModels {
    const val DEEPSEEK_R1_7B = "deepseek-r1:7B"

    const val LLAMA_3_2 = "llama3.2"

    const val MISTRAL_SMALL_22B = "mistral-small:22b"
    const val MISTRAL_SMALL_24B = "mistral-small:24b"

    const val QWEN_2_5_3B = "qwen2.5:3b"
    const val QWEN_2_5_7B = "qwen2.5:7b"
    const val QWEN_2_5_14B = "qwen2.5:14b"

    // https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-chat-completion
    @Serializable
    data class ChatRequest(
        val model: String,
        val messages: List<AiConversation.Message>,
        val stream: Boolean = true,
        val keep_alive: String = "10m",
        val options: Options = Options.DEFAULT,
        val tools: List<AiTool>? = null,
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
}
