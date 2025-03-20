package de.peekandpoke.aktor.frontend.common.markdown

import kotlin.js.Promise

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
