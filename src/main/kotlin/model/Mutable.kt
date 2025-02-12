package io.peekandpoke.aktor.model

class Mutable<T>(
    subject: T,
) {
    private val initialValue = subject

    var value = subject
        private set

    val isModified get() = value != initialValue

    fun getInitial() = initialValue

    fun modify(block: (T) -> T) {
        value = block(value)
    }
}
