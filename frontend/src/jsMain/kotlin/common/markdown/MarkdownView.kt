package de.peekandpoke.aktor.frontend.common.markdown

import de.peekandpoke.kraft.addons.styling.StyleSheetTag
import de.peekandpoke.kraft.addons.styling.StyleSheets
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.utils.SimpleAsyncQueue
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.unsafe

@Suppress("FunctionName")
fun Tag.MarkdownView(
    markdown: String,
) = comp(
    MarkdownView.Props(markdown = markdown)
) {
    MarkdownView(it)
}

class MarkdownView(ctx: Ctx<Props>) : Component<MarkdownView.Props>(ctx) {

    companion object {
        // https://cdnjs.com/libraries/highlight.js
        private val highlightStyle = StyleSheetTag {
            href = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/default.min.css"

            asDynamic().integrity =
                "sha512-hasIneQUHlh06VNBe7f6ZcHmeRTLIaQWFd43YriJ0UND19bvYRauxthDg8E4eVNPm9bRUhr5JGeqH7FRFXQu5g=="

            crossOrigin = "anonymous"
            referrerPolicy = "no-referrer"
        }

        private val katexStyle = StyleSheetTag {
            href = "https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.16.9/katex.min.css"

            asDynamic().integrity =
                "sha512-fHwaWebuwA7NSF5Qg/af4UeDx9XqUpYpOGgubo3yWu+b2IQR4UeQwbb42Ti7gVAjNtVoI/I9TEoYeu9omwcC6g=="

            crossOrigin = "anonymous"
            referrerPolicy = "no-referrer"
        }
    }

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val markdown: String,
    )

    private var pipeline: MarkdownRenderer? by value(null)

    private var rendered: String by value("")

    private val queue = SimpleAsyncQueue()

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                StyleSheets.mount(highlightStyle)
                StyleSheets.mount(katexStyle)

                queue.add { pipeline = MarkdownProcessor.getMarkdownRenderer() }
                queue.add { process() }
            }

            onNextProps { new, old ->
                if (new.markdown != old.markdown) {
                    queue.clear()
                    queue.add { process() }
                }
            }
        }
    }

    private suspend fun process() {
//        console.log("raw", props.markdown)

        // Modify markdown for remarkMath
        val cleaned = props.markdown
            .replace("\\\\[", "$$")
            .replace("\\\\]", "$$")
            .replace("\\[", "$$")
            .replace("\\]", "$$")
            .replace("\\\\(", "$")
            .replace("\\\\)", "$")
            .replace("\\(", "$")
            .replace("\\)", "$")

        try {
            rendered = pipeline!!(cleaned).await().value
        } catch (e: Exception) {
            console.error("Failed to render markdown", e)
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        div("markdown") {
            unsafe { +rendered }
        }
    }
}
