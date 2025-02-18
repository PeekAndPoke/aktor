package io.peekandpoke.aktor.llm

import io.peekandpoke.aktor.model.AiConversation
import io.peekandpoke.aktor.model.Mutable
import kotlinx.coroutines.flow.Flow

interface Llm {

    sealed interface Tool {
        data class Function(
            override val name: String,
            val description: String,
            val parameters: List<Param>,
            val fn: suspend (CallParams) -> String,
        ) : Tool {
            override suspend fun call(params: CallParams): String {
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

        data class StringParam(
            override val name: String,
            override val description: String,
            override val required: Boolean,
        ) : Param

        data class IntegerParam(
            override val name: String,
            override val description: String,
            override val required: Boolean,
        ) : Param

        data class BooleanParam(
            override val name: String,
            override val description: String,
            override val required: Boolean,
        ) : Param

        class CallParams(
            val params: Map<String, Any?>,
        ) {
            fun getString(name: String): String? = when(val x = params[name]) {
                null -> null
                is String -> x
                else -> x.toString()
            }

            fun getDouble(name: String): Double? = when(val x = params[name]) {
                null -> null
                is Number -> x.toDouble()
                else -> x.toString().toDoubleOrNull()
            }

            fun getInt(name: String): Int? = when(val x = params[name]) {
                null -> null
                is Number -> x.toInt()
                else -> x.toString().toIntOrNull()
            }

            fun getBoolean(name: String): Boolean? = when(val x = params[name]) {
                null -> null
                is Boolean -> x
                else -> x.toString().toBooleanStrictOrNull()
            }
        }

        val name: String

        suspend fun call(params: CallParams): String

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
    ): Flow<Update>

    val model: String
    val tools: List<Tool>
}

