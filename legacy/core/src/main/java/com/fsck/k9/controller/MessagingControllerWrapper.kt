package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.controller.MessagingListener
import com.fsck.k9.backend.api.Backend
import java.util.concurrent.Future
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
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
    private val accountManager: LegacyAccountDtoManager,
) {

    private fun getAccountDtoOrThrow(id: AccountId): LegacyAccountDto {
        return accountManager.getById(id) ?: error("Account not found: $id")
    }

    private fun getAccountDtoOrNull(id: AccountId): LegacyAccountDto? {
        return accountManager.getById(id)
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
        val account = getAccountDtoOrNull(id) ?: return false
        return messagingController.isMoveCapable(account)
    }

    fun isCopyCapable(id: AccountId): Boolean {
        val account = getAccountDtoOrNull(id) ?: return false
        return messagingController.isCopyCapable(account)
    }

    suspend fun moveMessagesInThread(
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

    suspend fun moveMessages(
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

    suspend fun copyMessagesInThread(
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

    suspend fun copyMessages(
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
        val account = id?.let { getAccountDtoOrNull(it) }

        messagingController.checkMail(
            account,
            ignoreLastCheckedTime,
            useManualWakeLock,
            notify,
            listener,
        )
    }

    fun supportsExpunge(id: AccountId): Boolean {
        val account = getAccountDtoOrNull(id) ?: return false
        return messagingController.supportsExpunge(account)
    }

    fun isPushCapable(id: AccountId): Boolean {
        val account = getAccountDtoOrNull(id) ?: return false
        return messagingController.isPushCapable(account)
    }

    suspend fun markAllMessagesRead(id: AccountId, folderId: Long) {
        val account = getAccountDtoOrThrow(id)
        messagingController.markAllMessagesRead(account, folderId)
    }

    fun checkAuthenticationProblem(id: AccountId) {
        val account = getAccountDtoOrThrow(id)
        // runBlocking preserves the synchronous behaviour of the former Java MessagingController.
        runBlocking { messagingController.checkAuthenticationProblem(account) }
    }

    fun isMoveCapable(message: MessageReference) = messagingController.isMoveCapable(message)
    fun isCopyCapable(message: MessageReference) = messagingController.isCopyCapable(message)

    suspend fun deleteThreads(messages: List<MessageReference>) = messagingController.deleteThreads(messages)

    suspend fun deleteMessages(messages: List<MessageReference>) = messagingController.deleteMessages(messages)
    suspend fun archiveThreads(messages: List<MessageReference>) = messagingController.archiveThreads(messages)
    suspend fun archiveMessages(messages: List<MessageReference>) = messagingController.archiveMessages(messages)
}
