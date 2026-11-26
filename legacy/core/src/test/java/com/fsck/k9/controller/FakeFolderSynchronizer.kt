package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessagingListener
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId

class FakeFolderSynchronizer : FolderSynchronizer {
    val synchronizedFolders = mutableListOf<Pair<AccountId, FolderId>>()
    val sentPendingMessages = mutableListOf<AccountId>()

    override fun synchronizeFolder(accountId: AccountId, folderId: FolderId, listener: MessagingListener?) {
        synchronizedFolders += accountId to folderId
    }

    override fun sendPendingMessages(accountId: AccountId, listener: MessagingListener?) {
        sentPendingMessages += accountId
    }
}
