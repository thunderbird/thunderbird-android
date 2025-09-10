package com.fsck.k9.ui.messagelist

import net.thunderbird.core.android.account.LegacyAccountDto

/**
 * Decides which folder to display when an account is selected.
 */
class DefaultFolderProvider {
    fun getDefaultFolder(account: LegacyAccountDto): Long {
        // Until the UI can handle the case where no remote folders have been fetched yet, we fall back to the Outbox
        // which should always exist.
        return account.autoExpandFolderId ?: account.inboxFolderId ?: account.outboxFolderId ?: error("Outbox missing")
    }
}
