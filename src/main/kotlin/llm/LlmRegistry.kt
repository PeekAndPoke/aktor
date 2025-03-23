package io.peekandpoke.aktor.llm

class LlmRegistry(
    val default: RegisteredLlm,
) {
    data class RegisteredLlm(
        val id: String,
        val description: String,
        val llm: Llm,
    )

    private val llms = mutableListOf<RegisteredLlm>(default)

    fun getById(id: String): RegisteredLlm {
        return llms.firstOrNull { it.id == id } ?: default
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
