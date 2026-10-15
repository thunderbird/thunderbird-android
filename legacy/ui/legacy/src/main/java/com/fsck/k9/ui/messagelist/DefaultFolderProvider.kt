package com.fsck.k9.ui.messagelist

import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

/**
 * Decides which folder to display when an account is selected.
 */
class DefaultFolderProvider(
    private val outboxFolderManager: OutboxFolderManager,
    private val accountManager: LegacyAccountManager,
) {
    fun getDefaultFolder(accountId: AccountId): Long {
        val account = accountManager.findById(accountId) ?: error("Account not found")
        // Until the UI can handle the case where no remote folders have been fetched yet, we fall back to the Outbox
        // which should always exist.
        return account.autoExpandFolderId
            ?: account.inboxFolderId
            ?: outboxFolderManager.getOutboxFolderIdSync(accountId).takeIf { it != -1L }
            ?: error("Outbox missing")
    }
}
