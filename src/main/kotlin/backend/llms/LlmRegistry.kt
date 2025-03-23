package io.peekandpoke.aktor.backend.llms

import io.peekandpoke.aktor.llm.Llm
import io.peekandpoke.aktor.shared.llms.model.RegisteredLlmModel

class LlmRegistry(
    val default: RegisteredLlm,
) {
    data class RegisteredLlm(
        val id: String,
        val description: String,
        val llm: Llm,
    ) {
        fun asApiModel() = RegisteredLlmModel(
            id = id,
            description = description,
        )
    }

    private val llms = mutableListOf<RegisteredLlm>(default)

    fun getAll(): List<RegisteredLlm> = llms.toList()

    fun getByIdOrDefault(id: String?): RegisteredLlm {
        return getById(id) ?: default
    }

    fun getById(id: String?): RegisteredLlm? {
        return id?.let {
            llms.firstOrNull { it.id == id }
        }
    }

    fun plus(id: String, description: String, llm: Llm) = apply {
        llms.add(
            RegisteredLlm(
                id = id,
                description = description,
                llm = llm,
            )
        )
    }
}
