package io.peekandpoke.reaktor.auth.db.karango

import io.peekandpoke.reaktor.auth.AuthStorage

class KarangoAuthStorage(
    override val authRecordsRepo: KarangoAuthRecordsRepo,
) : AuthStorage
