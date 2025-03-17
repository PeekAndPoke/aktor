package io.peekandpoke.aktor.examples

fun String.rotX(x: Int): String {
    return this.map { char ->
        when (char) {
            in 'A'..'Z' -> {
                val shifted = char + x
                if (shifted > 'Z') shifted - 26 else shifted
            }

            in 'a'..'z' -> {
                val shifted = char + x
                if (shifted > 'z') shifted - 26 else shifted
            }

            else -> char
        }
    }.joinToString("")
}
