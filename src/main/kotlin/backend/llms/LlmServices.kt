package io.peekandpoke.aktor.backend.llms

class LlmServices(
    registry: Lazy<LlmRegistry>,
) {
    val registry by registry
}
