package com.fsck.k9.ui.messagelist

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

/**
 * Decides which folder to display when an account is selected.
 */
class DefaultFolderProvider(
    private val outboxFolderManager: OutboxFolderManager,
) {
    fun getDefaultFolder(account: LegacyAccount): Long {
        // Until the UI can handle the case where no remote folders have been fetched yet, we fall back to the Outbox
        // which should always exist.
        return account.autoExpandFolderId
            ?: account.inboxFolderId
            ?: outboxFolderManager.getOutboxFolderIdSync(account.id).takeIf { it != -1L }
            ?: error("Outbox missing")
    }
}
