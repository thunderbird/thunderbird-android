package net.thunderbird.legacy.core.mailstore.folder

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

class FakeOutboxFolderManager(
    private val outboxIdMapping: MutableMap<AccountId, Long> = mutableMapOf(),
) : OutboxFolderManager {
    override suspend fun getOutboxFolderId(
        uuid: AccountId,
        createIfMissing: Boolean,
    ): Long {
        return if (createIfMissing) {
            outboxIdMapping.getOrPut(key = uuid) { 1L }
        } else {
            outboxIdMapping.getOrDefault(
                key = uuid,
                defaultValue = -1,
            )
        }
    }

    override suspend fun createOutboxFolder(uuid: AccountId): Outcome<Long, Exception> {
        val id = 1L
        outboxIdMapping[uuid] = id
        return Outcome.Success(id)
    }
}
