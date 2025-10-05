package io.peekandpoke.aktor.llms

import de.peekandpoke.ultra.kontainer.module
import io.peekandpoke.aktor.KeysConfig
import io.peekandpoke.aktor.backend.llms.LlmRegistry
import io.peekandpoke.aktor.backend.llms.LlmServices
import io.peekandpoke.aktor.backend.llms.api.LlmApiFeature
import io.peekandpoke.aktor.llms.anthropic.AnthropicLlm
import io.peekandpoke.aktor.llms.ollama.OllamaLlm
import io.peekandpoke.aktor.llms.ollama.OllamaModels
import io.peekandpoke.aktor.llms.openai.OpenAiLlm
import com.anthropic.models.messages.Model as AnthropicModel

val LlmsModule = module {

    singleton(LlmServices::class)
    singleton(LlmApiFeature::class)

    singleton(LlmRegistry::class) { keys: KeysConfig ->
        val default = LlmRegistry.RegisteredLlm(
            id = "openai/gpt-5-mini",
            description = "OpenAI GPT-5 mini",
            llm = OpenAiLlm(
                model = "gpt-5-mini",
                authToken = keys.config.getString("OPENAI_API_KEY"),
            )
        )

        LlmRegistry(default = default)
            .plus(
                id = "openai/gpt-5",
                description = "OpenAI GPT-5",
                llm = OpenAiLlm(
                    model = "gpt-5",
                    authToken = keys.config.getString("OPENAI_API_KEY"),
                )
            )
            .plus(
                id = "openai/gpt-5-mini",
                description = "OpenAI GPT-5 mini",
                llm = OpenAiLlm(
                    model = "gpt-5-mini",
                    authToken = keys.config.getString("OPENAI_API_KEY"),
                )
            )
            .plus(
                id = "openai/gpt-5-nano",
                description = "OpenAI GPT-5 nano",
                llm = OpenAiLlm(
                    model = "gpt-5-nano",
                    authToken = keys.config.getString("OPENAI_API_KEY"),
                )
            )
            .plus(
                id = "openai/gpt-4o-mini",
                description = "OpenAI GPT-4o mini",
                llm = OpenAiLlm(
                    model = "gpt-4o-mini",
                    authToken = keys.config.getString("OPENAI_API_KEY"),
                )
            )
            .plus(
                id = "openai/gpt-4o",
                description = "OpenAI GPT-4o",
                llm = OpenAiLlm(
                    model = "gpt-4o",
                    authToken = keys.config.getString("OPENAI_API_KEY"),
                )
            )
            .plus(
                id = "openai/o3-mini",
                description = "OpenAI o3-mini",
                llm = OpenAiLlm(
                    model = "o3-mini",
                    authToken = keys.config.getString("OPENAI_API_KEY"),
                )
            )
            .plus(
                id = "anthropic/claude-3-haiku",
                description = "Anthropic Claude 3 Haiku",
                llm = AnthropicLlm(
                    model = AnthropicModel.CLAUDE_3_HAIKU_20240307,
                    authToken = keys.config.getString("ANTHROPIC_API_KEY"),
                )
            )
            .plus(
                id = "anthropic/claude-3-opus",
                description = "Anthropic Claude 3 Opus",
                llm = AnthropicLlm(
                    model = AnthropicModel.CLAUDE_3_OPUS_LATEST,
                    authToken = keys.config.getString("ANTHROPIC_API_KEY"),
                )
            )
            .plus(
                id = "anthropic/claude-3.5-haiku",
                description = "Anthropic Claude 3.5 Haiku",
                llm = AnthropicLlm(
                    model = AnthropicModel.CLAUDE_3_5_HAIKU_LATEST,
                    authToken = keys.config.getString("ANTHROPIC_API_KEY"),
                )
            )
            .plus(
                id = "anthropic/claude-3.5-sonnet",
                description = "Anthropic Claude 3.5 Sonnet",
                llm = AnthropicLlm(
                    model = AnthropicModel.CLAUDE_3_5_SONNET_LATEST,
                    authToken = keys.config.getString("ANTHROPIC_API_KEY"),
                )
            )
            .plus(
                id = "anthropic/claude-3.7-sonnet",
                description = "Anthropic Claude 3.7 Sonnet",
                llm = AnthropicLlm(
                    model = AnthropicModel.CLAUDE_3_7_SONNET_LATEST,
                    authToken = keys.config.getString("ANTHROPIC_API_KEY"),
                )
            )
            .plus(
                id = "ollama/llama3.2:1b",
                description = "Ollama Llama 3.2 1B",
                llm = OllamaLlm(
                    model = OllamaModels.LLAMA_3_2_1B,
                )
            ).plus(
                id = "ollama/llama3.2:3b",
                description = "Ollama Llama 3.2 3B",
                llm = OllamaLlm(
                    model = OllamaModels.LLAMA_3_2_3B,
                )
            )
            .plus(
                id = "ollama/qwen2.5:7b",
                description = "Ollama Qwen 2.5 7B",
                llm = OllamaLlm(
                    model = OllamaModels.QWEN_2_5_7B,
                )
            )
    }
}
