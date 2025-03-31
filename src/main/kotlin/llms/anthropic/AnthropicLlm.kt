package io.peekandpoke.aktor.llms.anthropic

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.*
import com.anthropic.models.messages.*
import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.llms.LlmCommons
import io.peekandpoke.aktor.shared.model.Mutable
import io.peekandpoke.aktor.shared.model.Mutable.Companion.mutable
import io.peekandpoke.aktor.utils.unwrap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.asDeferred


class AnthropicLlm(
    val model: Model,
    private val authToken: String,
) : Llm, AutoCloseable {

    private val client by lazy { AnthropicOkHttpClient.builder().apiKey(authToken).build() }

    override fun getModelName() = model.value().name

    override fun close() {
        client.close()
    }

    override fun chat(
        conversation: AiConversation,
        tools: List<Llm.Tool>,
        streaming: Boolean,
    ): Flow<Llm.Update> {
        // TODO: streaming

        return chatNonStreaming(conversation.mutable(), tools)
    }

    private fun chatNonStreaming(
        conversation: Mutable<AiConversation>,
        tools: List<Llm.Tool>,
    ): Flow<Llm.Update> {

        return flow {
            var done = false

            while (!done) {
                val params = buildMessageCreateParams(conversation.value, tools)

                val result = client.async().messages().create(params).asDeferred().await()

                val content = result.content()
                    .mapNotNull { it.text().orElse(null)?.text() }
                    .joinToString()

                val toolCalls = result.content()
                    .mapNotNull { it.toolUse().orElse(null) }
                    .map { toolUse ->
                        val args = AiConversation.Message.ToolCall.Args.ofMap(
                            toolUse._input().asMapOrNull()
                        )

                        AiConversation.Message.ToolCall(
                            id = toolUse.id(),
                            name = toolUse.name(),
                            args = args,
                        )
                    }

                conversation.modify {
                    it.addOrUpdate(
                        AiConversation.Message.Assistant(
                            content = content.takeIf { it.isNotBlank() },
                            toolCalls = toolCalls.takeIf { it.isNotEmpty() },
                        )
                    )
                }

                emit(
                    Llm.Update.Response(conversation.value, content)
                )

                if (toolCalls.isEmpty()) {
                    done = true
                } else {
                    LlmCommons {
                        processToolCalls(conversation, tools, toolCalls)
                    }
                }
            }
        }
    }

    private fun buildMessageCreateParams(
        conversation: AiConversation,
        tools: List<Llm.Tool>,
    ): MessageCreateParams {
        val builder = MessageCreateParams.builder()
            .maxTokens(1024L)
            .model(model)

        tools.forEach { tool ->
            when (tool) {
                is Llm.Tool.Function -> {
                    val schema = tool.createJsonSchema()

                    builder.addTool(
                        Tool.builder()
                            .name(tool.name)
                            .description(tool.description)
                            .inputSchema(
                                JsonValue.from(schema.unwrap())
                            )
                            .build()
                    )
                }
            }
        }

        conversation.messages.forEach { msg ->

            // NOTICE: Anthropic does not allow empty messages
            when (msg) {
                is AiConversation.Message.System -> {
                    msg.takeIf { it.content.isNotBlank() }?.let { builder.system(msg.content) }
                }

                is AiConversation.Message.Assistant -> {
                    val blocks = listOfNotNull(
                        msg.content?.let { content ->
                            ContentBlockParam.ofText(
                                TextBlockParam.builder().text(content).build()
                            )
                        }
                    ).plus(
                        (msg.toolCalls ?: emptyList()).map { toolCall ->
                            ContentBlockParam.ofToolUse(
                                ToolUseBlockParam.builder()
                                    .id(toolCall.id)
                                    .name(toolCall.name)
                                    .input(JsonValue.from(toolCall.args))
                                    .build()
                            )
                        }
                    )

                    // TODO: do we need to check if the blocks are empty?
                    builder.addAssistantMessage(
                        MessageParam.Content.ofBlockParams(blocks)
                    )
                }

                is AiConversation.Message.User -> builder.addUserMessage(
                    msg.content.takeIf { it.isNotBlank() } ?: "-" // Anthropic does not allow empty messages
                )

                is AiConversation.Message.Tool -> {
                    val toolUse = ToolResultBlockParam.builder()
                        .toolUseId(msg.toolCall.id)
                        .content(msg.content)
                        .build()

                    val contentBlocks = MessageParam.Content.ofBlockParams(
                        listOf(ContentBlockParam.ofToolResult(toolUse))
                    )

                    builder.addUserMessage(contentBlocks)
                }
            }
        }

        val params = builder.build()

        return params
    }

    private fun JsonValue.asMapOrNull(): Map<String, Any?>? {
        @Suppress("UNCHECKED_CAST")
        return unwrap() as? Map<String, Any?>
    }

    private fun JsonValue.unwrap(): Any? {

        return when (this) {
            is JsonNull -> null
            is JsonMissing -> null
            is JsonObject -> values.mapValues { (_, value) -> value.unwrap() }
            is JsonArray -> values.map { it.unwrap() }
            is JsonBoolean -> value
            is JsonNumber -> value
            is JsonString -> value
        }
    }
}
