package io.peekandpoke.aktor.model

class Mutable<T>(
    subject: T,
) {
    companion object {
        fun <T> T.mutable() = Mutable(this)
    }

    private val initialValue = subject

    var value = subject
        private set

    val isModified get() = value != initialValue

    fun getInitial() = initialValue

    fun modify(block: (T) -> T) {
        value = block(value)
    }
}
