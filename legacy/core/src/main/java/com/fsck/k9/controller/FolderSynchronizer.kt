package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessagingListener
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId

/**
 * Synchronizes single folders of an account with the server.
 */
interface FolderSynchronizer {
    /**
     * Starts a background synchronization of the folder with the given [folderId].
     */
    fun synchronizeFolder(accountId: AccountId, folderId: FolderId, listener: MessagingListener?)

    /**
     * Starts sending the messages waiting in the account's outbox.
     */
    fun sendPendingMessages(accountId: AccountId, listener: MessagingListener?)
}
