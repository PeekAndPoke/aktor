package de.peekandpoke.aktor.frontend.common.markdown

import de.peekandpoke.kraft.utils.SimpleAsyncQueue
import kotlinext.js.js
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asDeferred
import kotlin.js.Promise

typealias MarkdownRenderer = (String) -> Deferred<String>

object MarkdownProcessor {

    private var pipelineCounter = 0
    private val queue = SimpleAsyncQueue()

    private val sanitizeConfig = js {
        tagNames = arrayOf(
            // Standard HTML tags
            "h1", "h2", "h3", "h4", "h5", "h6",
            "p", "br", "strong", "em", "code", "pre",
            "ul", "ol", "li", "blockquote",
            "a", "img", "div", "span",

            // SVG tags (safe subset)
            "svg", "path", "circle", "rect", "line", "polyline", "polygon",
            "ellipse", "g", "text", "tspan", "defs", "use", "symbol",
            "marker", "linearGradient", "radialGradient", "stop"
        )

        attributes = js {
            // Standard HTML attributes
            `*` = arrayOf("className", "id")
            a = arrayOf("href", "title", "target", "rel")
            img = arrayOf("src", "alt", "title", "width", "height")
            code = arrayOf("className", "data-lang")
            pre = arrayOf("className")
            span = arrayOf("style") // For KaTeX
            div = arrayOf("style") // For KaTeX/Mermaid

            // SVG attributes (safe subset)
            svg = arrayOf("width", "height", "viewBox", "xmlns", "className")
            path = arrayOf("d", "fill", "stroke", "strokeWidth", "className")
            circle = arrayOf("cx", "cy", "r", "fill", "stroke", "strokeWidth", "className")
            rect = arrayOf("x", "y", "width", "height", "fill", "stroke", "strokeWidth", "className")
            line = arrayOf("x1", "y1", "x2", "y2", "stroke", "strokeWidth", "className")
            g = arrayOf("fill", "stroke", "transform", "className")
            text = arrayOf("x", "y", "fill", "fontSize", "textAnchor", "className")
            use = arrayOf("href", "x", "y", "width", "height")
        }

        // Remove dangerous protocols
        protocols = js {
            href = arrayOf("http", "https", "mailto")
            src = arrayOf("http", "https", "data")
        }
    }


    fun getMarkdownRenderer(): MarkdownRenderer {

        return { input ->

            val deferred = CompletableDeferred<String>()

            queue.add {
                val pipeline = createNewMarkdownPipeline()

                try {
//                console.log("rendering", input)
                    val result = pipeline.process(input).asDeferred().await()
//                console.log("rendered", result.value)

                    val value = result.value

                    deferred.complete(value)
                } catch (e: Exception) {
                    deferred.completeExceptionally(e)
                }
            }

            deferred
        }
    }

    private suspend fun createNewMarkdownPipeline(): UnifiedModule.Unified {

        pipelineCounter++

        fun <T> load(block: () -> Promise<T>): Deferred<T> {
            return block().asDeferred()
        }

        val unifiedLoader = load<dynamic> { js("import('unified')") }
        val remarkParseLoader = load<dynamic> { js("import('remark-parse')") }
        val remarkGfmLoader = load<dynamic> { js("import('remark-gfm')") }
        val remarkMathLoader = load<dynamic> { js("import('remark-math')") }
        val remarkRehypeLoader = load<dynamic> { js("import('remark-rehype')") }
        val rehypeMermaidLoader = load<dynamic> { js("import('rehype-mermaid')") }
        val rehypeHighlightLoader = load<dynamic> { js("import('rehype-highlight')") }
        val rehypeKatexLoader = load<dynamic> { js("import('rehype-katex')") }
        val rehypeSanitizeLoader = load<dynamic> { js("import('rehype-sanitize')") }
        val rehypeStringifyLoader = load<dynamic> { js("import('rehype-stringify')") }

        val unified = unifiedLoader.await()
        val remarkParse = remarkParseLoader.await()
        val remarkGfm = remarkGfmLoader.await()
        val remarkMath = remarkMathLoader.await()
        val remarkRehype = remarkRehypeLoader.await()
        val rehypeMermaid = rehypeMermaidLoader.await()
        val rehypeHighlight = rehypeHighlightLoader.await()
        val rehypeKatex = rehypeKatexLoader.await()
        val rehypeSanitize = rehypeSanitizeLoader.await()
        val rehypeStringify = rehypeStringifyLoader.await()

        val pipeline = unified.unified()
            .use(remarkParse.default)
            .use(remarkGfm.default)
            .use(remarkMath.default)
            .use(remarkRehype.default)
            // IMPORTANT: sanitize is required to prevent XSS attacks BEFORE rehype-stringify
            .use(rehypeSanitize.default)
            .use(
                rehypeMermaid.default,
                js {
                    // https://github.com/remcohaszing/rehype-mermaid/blob/main/README.md?plain=1#L226
                    strategy = "img-png"
//                    strategy = "inline-svg"
                    // We need a fresh prefix everytime, otherwise rendering fails
                    prefix = "mermaid-${pipelineCounter}"
                    // Since we sometimes have partial code, we need to catch the errors
                    errorFallback = { element: dynamic, diagram: dynamic, error: dynamic, file: dynamic ->
                        console.warn("Failed to render mermaid", error, file)
                        // return
                        element
                    }
                }
            )
            .use(rehypeHighlight.default, js { ignoreMissing = true })
            .use(rehypeKatex.default)
            .use(rehypeStringify.default)

        return pipeline
    }
}
