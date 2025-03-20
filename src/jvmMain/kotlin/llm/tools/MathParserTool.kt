package io.peekandpoke.aktor.llm.tools

import io.peekandpoke.aktor.llm.Llm
import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression


/**
 * Tool for evaluating math expressions
 */
// https://github.com/mariuszgromada/MathParser.org-mXparser
class MathParserTool {

    fun asLlmTool(): Llm.Tool {
        return Llm.Tool.Function(
            name = "math_parser",
            description = """
                Evaluates mathematical expressions by using the mXparser library.
                
                Args:
                    expression: The expression to evaluate, f.e. "2+3*x"
                    arguments: Optional arguments to pass to the expression, f.e. "x=10,y=20"
                
                Returns: 
                    The result of the expression.
            """.trimIndent(),
            parameters = listOf(
                Llm.Tool.StringParam(
                    name = "expression",
                    description = "The expression to evaluate",
                    required = true
                )
            ),
            fn = { params ->
                val expression = params.getString("expression") ?: error("Missing parameter 'expression'")
                val arguments = params.getString("arguments")

                process(expression, arguments).toString()
            }
        )
    }

    fun process(expression: String, arguments: String?): Double {
        val args = (arguments?.split(",") ?: emptyList()).map { Argument(it) }
        val exp = Expression(expression, *args.toTypedArray())

        return exp.calculate()
    }
}
