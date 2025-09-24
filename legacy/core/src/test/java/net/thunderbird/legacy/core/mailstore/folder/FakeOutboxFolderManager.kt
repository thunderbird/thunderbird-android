package net.thunderbird.legacy.core.mailstore.folder

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

class FakeOutboxFolderManager @JvmOverloads constructor(
    private val outboxFolderId: Long = 1L,
    private val outboxIdMapping: MutableMap<AccountId, Long> = mutableMapOf(),
) : OutboxFolderManager {
    override suspend fun getOutboxFolderId(
        accountId: AccountId,
        createIfMissing: Boolean,
    ): Long {
        return if (createIfMissing) {
            outboxIdMapping.getOrPut(key = accountId) { outboxFolderId }
        } else {
            outboxIdMapping.getOrDefault(
                key = accountId,
                defaultValue = -1,
            )
        }
    }

    override suspend fun createOutboxFolder(accountId: AccountId): Outcome<Long, Exception> {
        outboxIdMapping[accountId] = outboxFolderId
        return Outcome.Success(outboxFolderId)
    }

    override suspend fun hasPendingMessages(accountId: AccountId): Boolean = accountId in outboxIdMapping
}
