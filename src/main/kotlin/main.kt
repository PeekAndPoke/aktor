package io.peekandpoke.aktor

import de.peekandpoke.ktorfx.core.AppKontainers
import de.peekandpoke.ktorfx.core.ktorFxApp

val app = ktorFxApp<AktorConfig>(
    kontainers = { config ->
        AppKontainers(
            createBlueprint(config)
        )
    },
)

fun main(args: Array<String>) = app.run(args)
