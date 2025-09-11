package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.controller.MessagingListener
import com.fsck.k9.mail.Flag
import java.util.concurrent.Future
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.account.AccountId

/**
 * A wrapper around [MessagingController] that takes care of loading the account by [AccountId] and
 * provides some convenience methods.
 */
@Suppress("TooManyFunctions")
class MessagingControllerWrapper(
    private val messagingController: MessagingController,
    private val accountManager: LegacyAccountDtoManager,
) {

    private fun getAccountDtoOrThrow(id: AccountId): LegacyAccountDto {
        return accountManager.getAccount(id.asRaw()) ?: error("Account not found: $id")
    }

    fun loadMoreMessages(id: AccountId, folderId: Long) {
        val account = getAccountDtoOrThrow(id)
        messagingController.loadMoreMessages(account, folderId)
    }

    fun loadSearchResults(
        id: AccountId,
        folderId: Long,
        messageServerIds: List<String>,
        listener: MessagingListener,
    ) {
        val account = getAccountDtoOrThrow(id)
        messagingController.loadSearchResults(account, folderId, messageServerIds, listener)
    }

    fun clearNewMessages(id: AccountId) {
        val account = getAccountDtoOrThrow(id)
        messagingController.clearNewMessages(account)
    }

    fun searchRemoteMessages(
        id: AccountId,
        folderId: Long,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        listener: MessagingListener,
    ): Future<*>? = messagingController.searchRemoteMessages(
        id.asRaw(),
        folderId,
        query,
        requiredFlags,
        forbiddenFlags,
        listener,
    )

    fun expunge(id: AccountId, folderId: Long) {
        val account = getAccountDtoOrThrow(id)
        messagingController.expunge(account, folderId)
    }

    fun sendPendingMessages(id: AccountId, listener: MessagingListener?) {
        val account = getAccountDtoOrThrow(id)
        messagingController.sendPendingMessages(account, listener)
    }

    fun setFlagForThreads(id: AccountId, threadIds: List<Long>, flag: Flag, newState: Boolean) {
        val account = getAccountDtoOrThrow(id)
        messagingController.setFlagForThreads(account, threadIds, flag, newState)
    }

    fun setFlag(id: AccountId, messageIds: List<Long>, flag: Flag, newState: Boolean) {
        val account = getAccountDtoOrThrow(id)
        messagingController.setFlag(account, messageIds, flag, newState)
    }

    fun isMoveCapable(id: AccountId): Boolean {
        val account = getAccountDtoOrThrow(id)
        return messagingController.isMoveCapable(account)
    }

    fun isCopyCapable(id: AccountId): Boolean {
        val account = getAccountDtoOrThrow(id)
        return messagingController.isCopyCapable(account)
    }

    fun moveMessagesInThread(
        id: AccountId,
        folderId: Long,
        messages: List<MessageReference>,
        destinationFolderId: Long,
    ) {
        val account = getAccountDtoOrThrow(id)
        messagingController.moveMessagesInThread(
            account,
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
        val account = getAccountDtoOrThrow(id)
        messagingController.moveMessages(
            account,
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
        val account = getAccountDtoOrThrow(id)
        messagingController.copyMessagesInThread(
            account,
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
        val account = getAccountDtoOrThrow(id)
        messagingController.copyMessages(
            account,
            folderId,
            messages,
            destinationFolderId,
        )
    }

    fun moveToDraftsFolder(id: AccountId, folderId: Long, messages: List<MessageReference>) {
        val account = getAccountDtoOrThrow(id)
        messagingController.moveToDraftsFolder(account, folderId, messages)
    }

    fun emptySpam(id: AccountId) {
        val account = getAccountDtoOrThrow(id)
        messagingController.emptySpam(account, null)
    }

    fun emptyTrash(id: AccountId) {
        val account = getAccountDtoOrThrow(id)
        messagingController.emptyTrash(account, null)
    }

    fun synchronizeMailbox(id: AccountId, folderId: Long, notify: Boolean, listener: MessagingListener?) {
        val account = getAccountDtoOrThrow(id)
        messagingController.synchronizeMailbox(account, folderId, notify, listener)
    }

    fun checkMail(
        id: AccountId?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    ) {
        val account = id?.let { getAccountDtoOrThrow(it) }

        messagingController.checkMail(
            account,
            ignoreLastCheckedTime,
            useManualWakeLock,
            notify,
            listener,
        )
    }

    fun supportsExpunge(id: AccountId): Boolean {
        val account = getAccountDtoOrThrow(id)
        return messagingController.supportsExpunge(account)
    }

    fun isPushCapable(id: AccountId): Boolean {
        val account = getAccountDtoOrThrow(id)
        return messagingController.isPushCapable(account)
    }

    fun markAllMessagesRead(id: AccountId, folderId: Long) {
        val account = getAccountDtoOrThrow(id)
        messagingController.markAllMessagesRead(account, folderId)
    }

    fun isMoveCapable(message: MessageReference) = messagingController.isMoveCapable(message)
    fun isCopyCapable(message: MessageReference) = messagingController.isCopyCapable(message)

    fun deleteThreads(messages: List<MessageReference>) = messagingController.deleteThreads(messages)

    fun deleteMessages(messages: List<MessageReference>) = messagingController.deleteMessages(messages)
    fun archiveThreads(messages: List<MessageReference>) = messagingController.archiveThreads(messages)
    fun archiveMessages(messages: List<MessageReference>) = messagingController.archiveMessages(messages)
}
