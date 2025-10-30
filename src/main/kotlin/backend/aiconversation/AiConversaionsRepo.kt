package io.peekandpoke.aktor.backend.aiconversation

import de.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import de.peekandpoke.karango.aql.*
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.peekandpoke.aktor.shared.aiconversation.model.AiConversationModel

inline val Database.aiConversations get() = getRepository(AiConversationsRepo::class)

class AiConversationsRepo(
    driver: KarangoDriver,
    onAfterSaves: List<OnAfterSave>,
    timestamps: TimestampedHook,
) : EntityRepository<AiConversation>(
    name = "ai_conversations",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamps.onBeforeSave())
) {
    companion object {
        fun Storable<AiConversation>.asApiModel(): AiConversationModel = with(value) {
            AiConversationModel(
                id = _id,
                ownerId = ownerId,
                messages = messages.map { it.asApiModel() },
                tools = tools,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }

    class Fixtures(
        repo: AiConversationsRepo,
    ) : RepoFixtureLoader<AiConversation>(repo = repo) {
    }

    interface OnAfterSave : Repository.Hooks.OnAfterSave<AiConversation>

    override fun IndexBuilder<AiConversation>.buildIndexes() {
        persistentIndex {
            field { ownerId }
        }

        persistentIndex {
            field { updatedAt.ts }
            field { ownerId }
        }
    }

    suspend fun findByOwner(ownerId: String, page: Int, epp: Int) = find {
        FOR(repo) { conversation ->
            FILTER(conversation.ownerId EQ ownerId.aql)

            SORT(conversation.updatedAt.ts.DESC)

            PAGE(page = page, epp = epp)

            RETURN(conversation)
        }
    }
}
