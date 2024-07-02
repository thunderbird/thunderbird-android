package com.fsck.k9.ui.helper

import com.fsck.k9.Account

object DisplayAddressHelper {
    fun shouldShowRecipients(account: Account, folderId: Long): Boolean {
        return when (folderId) {
            account.inboxFolderId -> false
            account.archiveFolderId -> false
            account.spamFolderId -> false
            account.trashFolderId -> false
            account.sentFolderId -> true
            account.draftsFolderId -> true
            account.outboxFolderId -> true
            else -> false
        }
    }
}
