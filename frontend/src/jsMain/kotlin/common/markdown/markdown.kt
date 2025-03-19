package de.peekandpoke.aktor.frontend.common.markdown

import de.peekandpoke.kraft.utils.ScriptLoader
import kotlinext.js.js
import kotlinx.html.FlowContent
import kotlin.js.Promise

fun FlowContent.renderMarkdown(content: String) {
    MarkdownView(content)
}

external object UnifiedModule {
    fun unified(): Unified

    interface Unified {
        fun use(fn: dynamic): Unified
        fun use(fn: dynamic, options: dynamic): Unified
        fun process(source: String): Promise<UnifiedResult>
    }

    interface UnifiedResult {
        var value: String
    }
}

suspend fun getMarkdownPipeline(): UnifiedModule.Unified {
    val unifiedLoader = ScriptLoader.load(
        ScriptLoader.Javascript.Module<UnifiedModule>(src = "https://esm.sh/unified@11?bundle")
    )

    val remarkParseLoader = ScriptLoader.load(
        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-parse@11?bundle")
    )

    val remarkMathLoader = ScriptLoader.load(
        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-math@6?bundle")
    )

    val remarkRehypeLoader = ScriptLoader.load(
        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-rehype@11?bundle")
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
    val remarkMath = remarkMathLoader.await()
    val remarkRehype = remarkRehypeLoader.await()
    val rehypeHighlight = rehypeHighlightLoader.await()
    val rehypeKatex = rehypeKatexLoader.await()
    val rehypeStringify = rehypeStringifyLoader.await()

//    console.log("Unified", unified)
//    console.log("Remark Parse", remarkParse)
//    console.log("Remark Math", remarkMath)
//    console.log("Remark Rehype", remarkRehype)
//    console.log("rehype-katex", rehypeKatex)
//    console.log("rehype-parse", rehypeParse)
//    console.log("rehype-format", rehypeFormat)
//    console.log("rehype-stringify", rehypeStringify)

    val pipeline = unified.module.unified()
        .use(remarkParse.module.default)
        .use(remarkMath.module.default)
        .use(remarkRehype.module.default)
        .use(rehypeHighlight.module.default, js { ignoreMissing = true })
        .use(rehypeKatex.module.default)
        .use(rehypeStringify.module.default)

    return pipeline
}
