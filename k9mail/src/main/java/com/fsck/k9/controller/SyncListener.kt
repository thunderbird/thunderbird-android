package com.fsck.k9.controller

import com.fsck.k9.mail.Message

interface SyncListener {
    fun syncStarted(folderServerId: String, folderName: String)

    fun syncHeadersStarted(folderServerId: String, folderName: String)
    fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int)
    fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int)

    fun syncProgress(folderServerId: String, completed: Int, total: Int)
    fun syncNewMessage(folderServerId: String, message: Message)
    fun syncRemovedMessage(folderServerId: String, message: Message)

    fun syncFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int)
    fun syncFailed(folderServerId: String, message: String)

    fun folderStatusChanged(folderServerId: String, unreadMessageCount: Int)
}
