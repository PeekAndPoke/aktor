package io.peekandpoke.aktor

import de.peekandpoke.funktor.core.AppKontainers
import de.peekandpoke.funktor.core.funktorApp

val app = funktorApp<AktorConfig>(
    kontainers = { config ->
        AppKontainers(
            createBlueprint(config)
        )
    },
)

fun main(args: Array<String>) = app.run(args)
