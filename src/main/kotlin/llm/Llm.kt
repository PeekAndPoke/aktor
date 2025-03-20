package io.peekandpoke.aktor.llm

import io.peekandpoke.aktor.model.Mutable
import io.peekandpoke.aktor.shared.model.AiConversation
import kotlinx.coroutines.flow.Flow

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
    }

    sealed interface Update {
        data class Response(
            val content: String?,
        ) : Update

        data class Info(
            val message: String,
        ) : Update

        data object Stop : Update
    }

    fun chat(
        conversation: Mutable<AiConversation>,
        streaming: Boolean,
    ): Flow<Update>

    val model: String
    val tools: List<Tool>
}

