package de.peekandpoke.funktor.auth.domain

import de.peekandpoke.karango.Karango
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.hooks.Timestamped
import kotlinx.serialization.SerialName

@Karango
data class AuthRecord(
    /** The realm that record belongs to */
    val realm: String,
    /** The id of the owner / user */
    val ownerId: String,
    /** The actual data */
    val entry: Entry,
    /** Epoch seconds timestamp, when this entry expires, or NULL if it never expires */
    val expiresAt: Long?,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {

    sealed interface Entry {
        @SerialName(Password.serialName)
        data class Password(
            val hash: String,
        ) : Entry {
            companion object {
                const val serialName = "password"
            }
        }
    }

    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
