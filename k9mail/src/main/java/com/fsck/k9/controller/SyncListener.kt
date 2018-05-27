package com.fsck.k9.controller

import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage

interface SyncListener {
    fun syncStarted(folderServerId: String, folderName: String)

    fun syncHeadersStarted(folderServerId: String, folderName: String)
    fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int)
    fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int)

    fun syncProgress(folderServerId: String, completed: Int, total: Int)
    // FIXME: Remove dependency on LocalMessage
    fun syncNewMessage(folderServerId: String, message: LocalMessage, previousUnreadMessageCount: Int)
    fun syncRemovedMessage(folderServerId: String, message: Message)

    fun syncFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int)
    fun syncFailed(folderServerId: String, message: String, exception: Exception?)

    fun folderStatusChanged(folderServerId: String, unreadMessageCount: Int)
}
