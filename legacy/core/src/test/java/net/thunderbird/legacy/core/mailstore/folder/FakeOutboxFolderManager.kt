package net.thunderbird.legacy.core.mailstore.folder

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

class FakeOutboxFolderManager @JvmOverloads constructor(
    private val outboxFolderId: Long = 1L,
    private val outboxIdMapping: MutableMap<AccountId, Long> = mutableMapOf(),
) : OutboxFolderManager {
    override suspend fun getOutboxFolderId(
        uuid: AccountId,
        createIfMissing: Boolean,
    ): Long {
        return if (createIfMissing) {
            outboxIdMapping.getOrPut(key = uuid) { outboxFolderId }
        } else {
            outboxIdMapping.getOrDefault(
                key = uuid,
                defaultValue = -1,
            )
        }
    }

    override suspend fun createOutboxFolder(uuid: AccountId): Outcome<Long, Exception> {
        outboxIdMapping[uuid] = outboxFolderId
        return Outcome.Success(outboxFolderId)
    }
}
