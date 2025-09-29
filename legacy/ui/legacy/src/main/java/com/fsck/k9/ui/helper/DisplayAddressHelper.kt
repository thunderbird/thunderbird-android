package com.fsck.k9.ui.helper

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

object DisplayAddressHelper {
    fun shouldShowRecipients(
        outboxFolderManager: OutboxFolderManager,
        account: LegacyAccount,
        folderId: Long,
    ): Boolean {
        return when (folderId) {
            account.inboxFolderId -> false
            account.archiveFolderId -> false
            account.spamFolderId -> false
            account.trashFolderId -> false
            account.sentFolderId -> true
            account.draftsFolderId -> true
            outboxFolderManager.getOutboxFolderIdSync(account.id) -> true
            else -> false
        }
    }
}
