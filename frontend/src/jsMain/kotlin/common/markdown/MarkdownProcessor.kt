package de.peekandpoke.aktor.frontend.common.markdown

import de.peekandpoke.kraft.utils.ScriptLoader
import de.peekandpoke.kraft.utils.SimpleAsyncQueue
import kotlinext.js.js
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asDeferred

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

        val unifiedLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<UnifiedModule>(src = "https://esm.sh/unified@11?bundle")
        )

        val remarkParseLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-parse@11?bundle")
        )

        val remarkGfmLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-gfm@4?bundle")
        )

        val remarkMathLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-math@6?bundle")
        )

        val remarkRehypeLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-rehype@11?bundle")
        )

        val rehypeMermaidLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/rehype-mermaid@3?bundle")
        )

        val rehypeHighlightLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/rehype-highlight@6?bundle")
        )

        val rehypeKatexLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/rehype-katex@7?bundle")
        )

        val rehypeStringifyLoader = ScriptLoader.load(
            ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/rehype-stringify@10?bundle")
        )

        val unified = unifiedLoader.await()
        val remarkParse = remarkParseLoader.await()
        val remarkGfm = remarkGfmLoader.await()
        val remarkMath = remarkMathLoader.await()
        val remarkRehype = remarkRehypeLoader.await()
        val rehypeMermaid = rehypeMermaidLoader.await()
        val rehypeHighlight = rehypeHighlightLoader.await()
        val rehypeKatex = rehypeKatexLoader.await()
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

        val pipeline = unified.module.unified()
            .use(remarkParse.module.default)
            .use(remarkGfm.module.default)
            .use(remarkMath.module.default)
            .use(remarkRehype.module.default)
            .use(
                rehypeMermaid.module.default,
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
            .use(rehypeHighlight.module.default, js { ignoreMissing = true })
            .use(rehypeKatex.module.default)
            .use(rehypeStringify.module.default)

        return pipeline
    }
}





