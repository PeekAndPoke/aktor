package de.peekandpoke.aktor.frontend.common.markdown

import de.peekandpoke.kraft.utils.SimpleAsyncQueue
import kotlinext.js.js
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asDeferred
import kotlin.js.Promise

typealias MarkdownRenderer = (String) -> Deferred<UnifiedModule.UnifiedResult>

object MarkdownProcessor {

    private var pipelineCounter = 0
    private val queue = SimpleAsyncQueue()

    fun getMarkdownRenderer(): MarkdownRenderer {

        return { input ->

            val deferred = CompletableDeferred<UnifiedModule.UnifiedResult>()

            queue.add {
                val pipeline = createNewMarkdownPipeline()

                try {
//                console.log("rendering", input)
                    val result = pipeline.process(input).asDeferred().await()
//                console.log("rendered", result.value)
                    deferred.complete(result)
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

//        fun createSafePlugin(plugin: dynamic): dynamic {
//            fun wrapper(vararg args: dynamic): dynamic {
//                console.log("plugin options", args)
//                val wrapped = plugin.call(null, args)
//
//                val fn: (tree: dynamic, file: dynamic) -> dynamic = { tree, file ->
//                    val promise = wrapped(tree, file)
//
//                    when (promise) {
//                        is Promise<*> -> promise.catch { error -> tree }
//                        else -> promise
//                    }
//                }
//
//                return fn
//            }
//
//            return ::wrapper
//        }

//    console.log("Unified", unified)
//    console.log("Remark Parse", remarkParse)
//    console.log("Remark Gfm", remarkGfm)
//    console.log("Remark Math", remarkMath)
//    console.log("Remark Rehype", remarkRehype)
//    console.log("mermaid", mermaid)
//    console.log("rehype-mermaid", rehypeMermaid)
//    console.log("rehype-katex", rehypeKatex)
//    console.log("rehype-parse", rehypeParse)
//    console.log("rehype-format", rehypeFormat)
//    console.log("rehype-stringify", rehypeStringify)

//    mermaid.module.default.initialize(
//        js {
//            startOnLoad = true
//        }
//    )

        val pipeline = unified.unified()
            .use(remarkParse.default)
            .use(remarkGfm.default)
            .use(remarkMath.default)
            .use(remarkRehype.default)
            .use(
                rehypeMermaid.default,
                js {
                    // https://github.com/remcohaszing/rehype-mermaid/blob/main/README.md?plain=1#L226
                    strategy = "img-png"
                    // We need a fresh prefix everytime, otherwise rendering fails
                    prefix = "mermaid-${pipelineCounter}"
                    // Since we sometimes have partial code, we need to catch the errors
                    errorFallback = { element: dynamic, diagram: dynamic, error: dynamic, file: dynamic ->
                        console.info("Failed to render mermaid", error, file)
                        // return
                        element
                    }
                }
            )
            .use(rehypeHighlight.default, js { ignoreMissing = true })
            .use(rehypeKatex.default)
            // IMPORTANT: sanitize is required to prevent XSS attacks BEFORE rehype-stringify
            .use(rehypeSanitize.default)
            .use(rehypeStringify.default)

        return pipeline
    }
}





