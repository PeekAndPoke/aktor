package io.peekandpoke.aktor.llm.tools

import de.peekandpoke.ultra.common.datetime.Kronos
import io.peekandpoke.aktor.llm.Llm

object ExampleLlmUtils {

    val getCurrentDateTimeTool = Llm.Tool.Function(
        name = "get_current_datetime",
        description = """
            Gets the current date and time.
                            
            Returns:
                Date and time in the format `EEEE yyyy-MM-dd HH:mm:ss` or `weekday date time`
        """.trimIndent(),
        parameters = emptyList(),
        fn = {
            Kronos.systemUtc.instantNow().atSystemDefaultZone().format("EEEE yyyy-MM-dd HH:mm:ss")
        }
    )

    val getUsersNameTool = Llm.Tool.Function(
        name = "get_users_name",
        description = """
            Gets the of the Human interacting with the assistant.
            
            This is NOT the name of the bot.
            
            Returns:
            the users name like `Firstname Lastname`
        """.trimIndent(),
        parameters = emptyList(),
        fn = {
            "Karsten Gerber"
        }
    )

    val encryptTool = Llm.Tool.Function(
        name = "encrypt_text",
        description = """
            Encrypts the given text.
            
            Always use this tool for encryption. Never guess the encryption!
            Never remember the result!
            
            Returns: 
            The encrypted text as a string.
        """.trimIndent(),
        parameters = listOf(
            Llm.Tool.StringParam(name = "text", description = "The text to encrypt.", required = true),
        ),
        fn = { params ->
            val text = params.getString("text") ?: error("Missing parameter 'text'")

            text.rotX(13)
        }
    )

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
}
