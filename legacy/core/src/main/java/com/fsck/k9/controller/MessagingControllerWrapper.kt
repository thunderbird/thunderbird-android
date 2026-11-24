package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.controller.MessagingListener
import com.fsck.k9.backend.api.Backend
import java.util.concurrent.Future
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.account.AccountId

/**
 * A wrapper around [MessagingController] that takes care of loading the account by [AccountId] and
 * provides some convenience methods.
 */
@Suppress("TooManyFunctions")
class MessagingControllerWrapper(
    private val messagingController: MessagingController,
) {

    fun loadMoreMessages(accountId: AccountId, folderId: Long) {
        messagingController.loadMoreMessages(accountId, folderId)
    }

    fun loadSearchResults(
        accountId: AccountId,
        folderId: Long,
        messageServerIds: List<String>,
        listener: MessagingListener,
    ) {
        messagingController.loadSearchResults(accountId, folderId, messageServerIds, listener)
    }

    fun clearNewMessages(accountId: AccountId) {
        messagingController.clearNewMessages(accountId)
    }

    fun searchRemoteMessages(
        accountId: AccountId,
        folderId: Long,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        listener: MessagingListener,
    ): Future<*>? = messagingController.searchRemoteMessages(
        accountId,
        folderId,
        query,
        requiredFlags,
        forbiddenFlags,
        listener,
    )

    fun expunge(id: AccountId, folderId: Long) {
        messagingController.expunge(id, folderId)
    }

    fun sendPendingMessages(accountId: AccountId, listener: MessagingListener?) {
        messagingController.sendPendingMessages(accountId, listener)
    }

    fun setFlagForThreads(accountId: AccountId, threadIds: List<Long>, flag: Flag, newState: Boolean) {
        messagingController.setFlagForThreads(accountId, threadIds, flag, newState)
    }

    fun setFlag(accountId: AccountId, messageIds: List<Long>, flag: Flag, newState: Boolean) {
        messagingController.setFlag(accountId, messageIds, flag, newState)
    }

    fun isMoveCapable(accountId: AccountId): Boolean {
        return messagingController.isMoveCapable(accountId)
    }

    fun isCopyCapable(accountId: AccountId): Boolean {
        return messagingController.isCopyCapable(accountId)
    }

    fun moveMessagesInThread(
        id: AccountId,
        folderId: Long,
        messages: List<MessageReference>,
        destinationFolderId: Long,
    ) {
        messagingController.moveMessagesInThread(
            id,
            folderId,
            messages,
            destinationFolderId,
        )
    }

    fun moveMessages(
        id: AccountId,
        folderId: Long,
        messages: List<MessageReference>,
        destinationFolderId: Long,
    ) {
        messagingController.moveMessages(
            id,
            folderId,
            messages,
            destinationFolderId,
        )
    }

    fun copyMessagesInThread(
        id: AccountId,
        folderId: Long,
        messages: List<MessageReference>,
        destinationFolderId: Long,
    ) {
        messagingController.copyMessagesInThread(
            id,
            folderId,
            messages,
            destinationFolderId,
        )
    }

    fun copyMessages(
        id: AccountId,
        folderId: Long,
        messages: List<MessageReference>,
        destinationFolderId: Long,
    ) {
        messagingController.copyMessages(
            id,
            folderId,
            messages,
            destinationFolderId,
        )
    }

    fun moveToDraftsFolder(id: AccountId, folderId: Long, messages: List<MessageReference>) {
        messagingController.moveToDraftsFolder(id, folderId, messages)
    }

    fun emptySpam(id: AccountId) {
        messagingController.emptySpam(id, null)
    }

    fun emptyTrash(id: AccountId) {
        messagingController.emptyTrash(id, null)
    }

    fun synchronizeMailbox(id: AccountId, folderId: Long, notify: Boolean, listener: MessagingListener?) {
        messagingController.synchronizeMailbox(id, folderId, notify, listener)
    }

    fun checkMail(
        id: AccountId?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    ) {
        messagingController.checkMail(
            id,
            ignoreLastCheckedTime,
            useManualWakeLock,
            notify,
            listener,
        )
    }

    fun supportsExpunge(accountId: AccountId): Boolean {
        return messagingController.supportsExpunge(accountId)
    }

    fun isPushCapable(accountId: AccountId): Boolean {
        return messagingController.isPushCapable(accountId)
    }

    fun markAllMessagesRead(accountId: AccountId, folderId: Long) {
        messagingController.markAllMessagesRead(accountId, folderId)
    }

    fun checkAuthenticationProblem(accountId: AccountId) {
        messagingController.checkAuthenticationProblem(accountId)
    }

    fun isMoveCapable(message: MessageReference) = messagingController.isMoveCapable(message)
    fun isCopyCapable(message: MessageReference) = messagingController.isCopyCapable(message)

    fun deleteThreads(messages: List<MessageReference>) = messagingController.deleteThreads(messages)

    fun deleteMessages(messages: List<MessageReference>) = messagingController.deleteMessages(messages)
    fun archiveThreads(messages: List<MessageReference>) = messagingController.archiveThreads(messages)
    fun archiveMessages(messages: List<MessageReference>) = messagingController.archiveMessages(messages)
}

@Throws(MessagingException::class)
internal fun Backend.downloadCompleteMessageBlocking(
    ioDispatcher: CoroutineDispatcher,
    folderServerId: String,
    messageServerId: String,
) {
    runBlocking(ioDispatcher) {
        downloadCompleteMessage(folderServerId, messageServerId)
    }
}
