package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageCounts
import app.k9mail.legacy.message.controller.MessageCountsProvider
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import com.fsck.k9.search.getLegacyAccounts
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.legacy.logging.Log
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount
import net.thunderbird.feature.search.legacy.SearchConditionTreeNode
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition

internal class DefaultMessageCountsProvider(
    private val accountManager: LegacyAccountManager,
    private val messageStoreManager: MessageStoreManager,
    private val messagingControllerRegistry: MessagingControllerRegistry,
    private val outboxFolderManager: OutboxFolderManager,
    private val logger: Logger,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : MessageCountsProvider {
    override fun getMessageCounts(accountId: AccountId): MessageCounts {
        val account = accountManager.findById(accountId) ?: error("Account with id $accountId not found")
        val search = LocalMessageSearch().apply {
            excludeSpecialFolders(account, outboxFolderId = outboxFolderManager.getOutboxFolderIdSync(accountId))
            limitToDisplayableFolders()
        }

        return getMessageCounts(accountId, search.conditions)
    }

    override fun getMessageCounts(searchAccount: SearchAccount): MessageCounts {
        return getMessageCounts(searchAccount.relatedSearch)
    }

    override fun getMessageCounts(search: LocalMessageSearch): MessageCounts {
        val accounts = search.getLegacyAccounts(accountManager)

        var unreadCount = 0
        var starredCount = 0
        for (account in accounts) {
            val accountMessageCount = getMessageCounts(account.id, search.conditions)
            unreadCount += accountMessageCount.unread
            starredCount += accountMessageCount.starred
        }

        return MessageCounts(unreadCount, starredCount)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun getUnreadMessageCount(accountId: AccountId, folderId: Long): Int {
        return try {
            val messageStore = messageStoreManager.getMessageStore(accountId)
            val outboxFolderId = outboxFolderManager.getOutboxFolderIdSync(accountId)
            return if (folderId == outboxFolderId) {
                messageStore.getMessageCount(folderId)
            } else {
                messageStore.getUnreadMessageCount(folderId)
            }
        } catch (e: Exception) {
            logger.error(throwable = e) { "Unable to getUnreadMessageCount for account: $accountId, folder: $folderId" }
            0
        }
    }

    override fun getMessageCountsFlow(search: LocalMessageSearch): Flow<MessageCounts> {
        return callbackFlow {
            send(getMessageCounts(search))

            val folderStatusChangedListener = object : SimpleMessagingListener() {
                override fun folderStatusChanged(accountId: AccountId, folderId: Long) {
                    trySendBlocking(getMessageCounts(search))
                }
            }
            messagingControllerRegistry.addListener(folderStatusChangedListener)

            awaitClose {
                messagingControllerRegistry.removeListener(folderStatusChangedListener)
            }
        }.buffer(capacity = Channel.CONFLATED)
            .distinctUntilChanged()
            .flowOn(coroutineContext)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getMessageCounts(accountId: AccountId, conditions: SearchConditionTreeNode?): MessageCounts {
        return try {
            val messageStore = messageStoreManager.getMessageStore(accountId)
            return MessageCounts(
                unread = messageStore.getUnreadMessageCount(conditions),
                starred = messageStore.getStarredMessageCount(conditions),
            )
        } catch (e: Exception) {
            Log.e(e, "Unable to getMessageCounts for account: %s", accountId)
            MessageCounts(unread = 0, starred = 0)
        }
    }

    /**
     * Modify the supplied [LocalMessageSearch] instance to limit the search to displayable folders.
     */
    private fun LocalMessageSearch.limitToDisplayableFolders() {
        and(
            MessageSearchField.VISIBLE,
            "1",
            SearchAttribute.EQUALS,
        )
    }

    /**
     * Modify the supplied [LocalMessageSearch] instance to exclude special folders.
     *
     * Currently the following folders are excluded:
     *  - Trash
     *  - Drafts
     *  - Spam
     *  - Outbox
     *  - Sent
     *
     * The Inbox will always be included even if one of the special folders is configured to point to the Inbox.
     */
    private fun LocalMessageSearch.excludeSpecialFolders(account: LegacyAccount, outboxFolderId: Long) {
        this.excludeSpecialFolder(account.trashFolderId)
        this.excludeSpecialFolder(account.draftsFolderId)
        this.excludeSpecialFolder(account.spamFolderId)
        this.excludeSpecialFolder(outboxFolderId)
        this.excludeSpecialFolder(account.sentFolderId)

        account.inboxFolderId?.let { inboxFolderId ->
            or(
                SearchCondition(
                    MessageSearchField.FOLDER,
                    SearchAttribute.EQUALS,
                    inboxFolderId.toString(),
                ),
            )
        }
    }

    private fun LocalMessageSearch.excludeSpecialFolder(folderId: Long?) {
        if (folderId != null) {
            and(
                MessageSearchField.FOLDER,
                folderId.toString(),
                SearchAttribute.NOT_EQUALS,
            )
        }
    }
}
