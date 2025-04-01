package io.peekandpoke.aktor.llms.anthropic

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.*
import com.anthropic.core.http.StreamResponse
import com.anthropic.helpers.MessageAccumulator
import com.anthropic.models.messages.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import de.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.llms.Llm
import io.peekandpoke.aktor.llms.LlmCommons
import io.peekandpoke.aktor.shared.model.Mutable
import io.peekandpoke.aktor.shared.model.Mutable.Companion.mutable
import io.peekandpoke.aktor.utils.unwrap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.stream.consumeAsFlow
import kotlin.streams.asSequence

class AnthropicLlm(
    val model: Model,
    private val authToken: String,
) : Llm, AutoCloseable {

    companion object {
        private val mapper = ObjectMapper()
    }

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
        return if (streaming) {
            chatStreaming(conversation.mutable(), tools)
        } else {
            chatNonStreaming(conversation.mutable(), tools)
        }
    }

    private fun chatStreaming(
        conversation: Mutable<AiConversation>,
        tools: List<Llm.Tool>,
    ): Flow<Llm.Update> {
        var done = false

        return flow {
            emit(
                Llm.Update.Start(conversation.value)
            )

            while (!done) {
                var msgAccu = AiConversation.Message.Assistant()
                val chunkAccu: MessageAccumulator = MessageAccumulator.create()

                val params: MessageCreateParams = buildMessageCreateParams(conversation.value, tools)
                val response: StreamResponse<RawMessageStreamEvent> = client.messages().createStreaming(params)

                response.stream().consumeAsFlow()
                    .collect { chunk ->
                        try {
                            chunkAccu.accumulate(chunk)
                        } catch (_: MismatchedInputException) {
                            // ignore this one, happens when no content is received
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        val contentDelta = chunk.contentBlockDelta().stream()
                            .flatMap { it.delta().text().stream() }
                            .asSequence()
                            .joinToString("") { it.text() }

                        msgAccu = msgAccu.appendContent(contentDelta)

                        conversation.modify { it.addOrUpdate(msgAccu) }

                        emit(
                            Llm.Update.Response(conversation.value, contentDelta)
                        )
                    }

                // Get the final version of the message
                val message: Message = chunkAccu.message()

                msgAccu = msgAccu.copy(
                    content = message.extractTextContent(),
                    toolCalls = message.extractToolCalls(),
                    rawResponse = message.convertToJson(),
                )

                conversation.modify {
                    it.addOrUpdate(msgAccu)
                }

                emit(
                    Llm.Update.Response(conversation.value, msgAccu.content)
                )

                val toolCalls = msgAccu.toolCalls

                if (toolCalls == null || toolCalls.isEmpty()) {
                    done = true
                } else {
                    LlmCommons {
                        processToolCalls(conversation, tools, toolCalls)
                    }
                }
            }
        }
    }

    private fun chatNonStreaming(
        conversation: Mutable<AiConversation>,
        tools: List<Llm.Tool>,
    ): Flow<Llm.Update> {
        var done = false

        return flow {
            emit(
                Llm.Update.Start(conversation.value)
            )

            while (!done) {

                val params = buildMessageCreateParams(conversation.value, tools)

                val message = client.async().messages().create(params).asDeferred().await()

                val content = message.extractTextContent()
                val toolCalls = message.extractToolCalls()

                conversation.modify {
                    it.addOrUpdate(
                        AiConversation.Message.Assistant(
                            content = content,
                            toolCalls = toolCalls,
                            rawResponse = message.convertToJson(),
                        )
                    )
                }

                emit(
                    Llm.Update.Response(conversation.value, content)
                )

                if (toolCalls == null || toolCalls.isEmpty()) {
                    done = true
                } else {
                    LlmCommons {
                        processToolCalls(conversation, tools, toolCalls)
                    }
                }
            }
        }
    }

    private fun Message.extractTextContent() = content().extractTextContent()

    private fun List<ContentBlock>.extractTextContent() = mapNotNull { it.text().orElse(null)?.text() }
        .joinToString()
        .takeIf { it.isNotBlank() }

    private fun Message.extractToolCalls() = content().extractToolCalls()

    private fun List<ContentBlock>.extractToolCalls() = mapNotNull { it.toolUse().orElse(null) }
        .map { toolUse ->
            val args = AiConversation.Message.ToolCall.Args.ofMap(
                toolUse._input().asMapOrNull()
            )

            AiConversation.Message.ToolCall(
                id = toolUse.id(),
                name = toolUse.name(),
                args = args,
            )
        }.takeIf { it.isNotEmpty() }

    private fun Message.convertToJson() = mapper
        .convertValue(this, HashMap::class.java)
        .toMap().toJsonElement()

    private fun buildMessageCreateParams(
        conversation: AiConversation,
        tools: List<Llm.Tool>,
    ): MessageCreateParams {
        val builder = MessageCreateParams.builder()
            .maxTokens(2048L)
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

            when (msg) {
                is AiConversation.Message.System -> {
                    // Anthropic does not allow empty content
                    msg.takeIf { it.content.isNotBlank() }?.let { builder.system(msg.content) }
                }

                is AiConversation.Message.Assistant -> {
                    val blocks = listOfNotNull(
                        // Anthropic does not allow empty content
                        msg.content?.takeIf { it.isNotBlank() }?.let { content ->
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

                    builder.addAssistantMessage(
                        MessageParam.Content.ofBlockParams(blocks)
                    )
                }

                is AiConversation.Message.User -> {
                    val blocks = listOfNotNull(
                        // Anthropic does not allow empty content
                        msg.content.takeIf { it.isNotBlank() }?.let { content ->
                            ContentBlockParam.ofText(
                                TextBlockParam.builder().text(content).build()
                            )
                        }
                    )

                    builder.addUserMessage(
                        MessageParam.Content.ofBlockParams(blocks)
                    )
                }

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
