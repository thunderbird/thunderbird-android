package com.fsck.k9.backend.api

interface SyncListener {
    fun syncStarted(folderServerId: String)

    fun syncAuthenticationSuccess()

    fun syncHeadersStarted(folderServerId: String)
    fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int)
    fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int)

    fun syncProgress(folderServerId: String, completed: Int, total: Int)
    fun syncNewMessage(folderServerId: String, messageServerId: String, isOldMessage: Boolean)
    fun syncRemovedMessage(folderServerId: String, messageServerId: String)
    fun syncFlagChanged(folderServerId: String, messageServerId: String)

    fun syncFinished(folderServerId: String)
    fun syncFailed(folderServerId: String, message: String, exception: Exception?)

    fun folderStatusChanged(folderServerId: String)
}
