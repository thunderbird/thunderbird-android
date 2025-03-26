package com.fsck.k9.ui.helper

import app.k9mail.legacy.account.LegacyAccount

object DisplayAddressHelper {
    fun shouldShowRecipients(account: LegacyAccount, folderId: Long): Boolean {
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
