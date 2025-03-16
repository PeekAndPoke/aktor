package de.peekandpoke.aktor.frontend.common

import de.peekandpoke.kraft.addons.marked.marked
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.unsafe

fun FlowContent.renderMarkdown(content: String) {
    val md = marked.parse(content)

    div("markdown") {
        unsafe { +md }
    }
}

