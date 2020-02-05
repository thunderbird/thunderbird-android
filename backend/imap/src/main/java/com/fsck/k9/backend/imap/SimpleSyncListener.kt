package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.SyncListener

class SimpleSyncListener : SyncListener {
    override fun syncStarted(folderServerId: String) = Unit
    override fun syncAuthenticationSuccess() = Unit
    override fun syncHeadersStarted(folderServerId: String) = Unit
    override fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int) = Unit
    override fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int) = Unit
    override fun syncProgress(folderServerId: String, completed: Int, total: Int) = Unit
    override fun syncNewMessage(folderServerId: String, messageServerId: String, isOldMessage: Boolean) = Unit
    override fun syncRemovedMessage(folderServerId: String, messageServerId: String) = Unit
    override fun syncFlagChanged(folderServerId: String, messageServerId: String) = Unit
    override fun syncFinished(folderServerId: String) = Unit
    override fun syncFailed(folderServerId: String, message: String, exception: Exception?) = Unit
    override fun folderStatusChanged(folderServerId: String) = Unit
}
