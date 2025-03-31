package de.peekandpoke.funktor.auth.db.karango

import de.peekandpoke.funktor.auth.AuthStorage

class KarangoAuthStorage(
    override val authRecordsRepo: KarangoAuthRecordsRepo,
) : AuthStorage
