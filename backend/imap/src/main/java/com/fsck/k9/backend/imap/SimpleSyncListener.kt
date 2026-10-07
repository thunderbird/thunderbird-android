package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.SyncListener

class SimpleSyncListener : SyncListener {
    override suspend fun syncStarted(folderServerId: String) = Unit
    override fun syncAuthenticationSuccess() = Unit
    override fun syncHeadersStarted(folderServerId: String) = Unit
    override fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int) = Unit
    override fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int) = Unit
    override suspend fun syncProgress(folderServerId: String, completed: Int, total: Int) = Unit
    override suspend fun syncNewMessage(folderServerId: String, messageServerId: String, isOldMessage: Boolean) = Unit
    override suspend fun syncRemovedMessage(folderServerId: String, messageServerId: String) = Unit
    override suspend fun syncFlagChanged(folderServerId: String, messageServerId: String) = Unit
    override suspend fun syncFinished(folderServerId: String) = Unit
    override suspend fun syncFailed(folderServerId: String, message: String, exception: Exception?) = Unit
    override suspend fun folderStatusChanged(folderServerId: String) = Unit
}
