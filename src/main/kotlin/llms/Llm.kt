package io.peekandpoke.aktor.llm

import io.peekandpoke.aktor.backend.aiconversation.AiConversation
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel.ToolRef
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.*

interface Llm {

    sealed interface Tool {
        data class Function(
            override val name: String,
            val description: String,
            val parameters: List<Param>,
            val fn: suspend (AiConversation.Message.ToolCall.Args) -> String,
        ) : Tool {
            override suspend fun call(params: AiConversation.Message.ToolCall.Args): String {
                return fn(params)
            }

            override fun describe(): String {
                return "${name}(${parameters.joinToString(", ") { it.name }}) - $description"
            }

            override fun toToolRef(): ToolRef {
                return ToolRef(
                    name = name,
                    description = description,
                    parameters = parameters.associate {
                        val description = listOfNotNull(
                            it::class.simpleName,
                            it.required.takeIf { !it }?.let { "[OPTIONAL]" },
                            it.description,
                        ).joinToString(" ")

                        it.name to description
                    }
                )
            }

            fun createJsonSchema(): JsonObject = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    parameters.forEach { p ->
                        when (p) {
                            is StringParam -> putJsonObject(p.name) {
                                put("type", "string")
                                put("description", p.description)
                            }

                            is NumberParam -> putJsonObject(p.name) {
                                put("type", "number")
                                put("description", p.description)
                            }

                            is IntegerParam -> putJsonObject(p.name) {
                                put("type", "integer")
                                put("description", p.description)
                            }

                            is BooleanParam -> putJsonObject(p.name) {
                                put("type", "boolean")
                                put("description", p.description)
                            }
                        }
                    }
                }
                putJsonArray("required") {
                    parameters.filter { it.required }.forEach { p ->
                        add(p.name)
                    }
                }
            }
        }

        sealed interface Param {
            val name: String
            val description: String
            val required: Boolean
        }

        data class BooleanParam(
            override val name: String,
            override val description: String,
            override val required: Boolean,
        ) : Param

        data class IntegerParam(
            override val name: String,
            override val description: String,
            override val required: Boolean,
        ) : Param

        data class NumberParam(
            override val name: String,
            override val description: String,
            override val required: Boolean,
        ) : Param

        data class StringParam(
            override val name: String,
            override val description: String,
            override val required: Boolean,
        ) : Param

        val name: String

        suspend fun call(params: AiConversation.Message.ToolCall.Args): String

        fun describe(): String

        fun toToolRef(): ToolRef
    }

    sealed interface Update {
        data class Start(
            override val conversation: AiConversation,
        ) : Update

        data class Response(
            override val conversation: AiConversation,
            val content: String?,
        ) : Update

        data class Info(
            override val conversation: AiConversation,
            val message: String,
        ) : Update

        data class Stop(
            override val conversation: AiConversation,
        ) : Update

        val conversation: AiConversation
    }

    fun chat(
        conversation: AiConversation,
        tools: List<Tool>,
        streaming: Boolean,
    ): Flow<Update>

    fun getModelName(): String
}

