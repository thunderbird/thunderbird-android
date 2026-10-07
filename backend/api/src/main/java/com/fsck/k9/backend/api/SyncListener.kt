package com.fsck.k9.backend.api

interface SyncListener {
    suspend fun syncStarted(folderServerId: String)

    fun syncAuthenticationSuccess()

    fun syncHeadersStarted(folderServerId: String)
    fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int)
    fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int)

    suspend fun syncProgress(folderServerId: String, completed: Int, total: Int)
    suspend fun syncNewMessage(folderServerId: String, messageServerId: String, isOldMessage: Boolean)
    suspend fun syncRemovedMessage(folderServerId: String, messageServerId: String)
    suspend fun syncFlagChanged(folderServerId: String, messageServerId: String)

    suspend fun syncFinished(folderServerId: String)
    suspend fun syncFailed(folderServerId: String, message: String, exception: Exception?)

    suspend fun folderStatusChanged(folderServerId: String)
}
