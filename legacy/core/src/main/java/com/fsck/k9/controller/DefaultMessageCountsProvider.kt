package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageCounts
import app.k9mail.legacy.message.controller.MessageCountsProvider
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import com.fsck.k9.search.excludeSpecialFolders
import com.fsck.k9.search.getAccounts
import com.fsck.k9.search.limitToDisplayableFolders
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
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount
import net.thunderbird.feature.search.legacy.SearchConditionTreeNode

internal class DefaultMessageCountsProvider(
    private val accountManager: LegacyAccountDtoManager,
    private val messageStoreManager: MessageStoreManager,
    private val messagingControllerRegistry: MessagingControllerRegistry,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : MessageCountsProvider {
    override fun getMessageCounts(account: LegacyAccountDto): MessageCounts {
        val search = LocalMessageSearch().apply {
            excludeSpecialFolders(account)
            limitToDisplayableFolders()
        }

        return getMessageCounts(account, search.conditions)
    }

    override fun getMessageCounts(searchAccount: SearchAccount): MessageCounts {
        return getMessageCounts(searchAccount.relatedSearch)
    }

    override fun getMessageCounts(search: LocalMessageSearch): MessageCounts {
        val accounts = search.getAccounts(accountManager)

        var unreadCount = 0
        var starredCount = 0
        for (account in accounts) {
            val accountMessageCount = getMessageCounts(account, search.conditions)
            unreadCount += accountMessageCount.unread
            starredCount += accountMessageCount.starred
        }

        return MessageCounts(unreadCount, starredCount)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun getUnreadMessageCount(account: LegacyAccountDto, folderId: Long): Int {
        return try {
            val messageStore = messageStoreManager.getMessageStore(account)
            return if (folderId == account.outboxFolderId) {
                messageStore.getMessageCount(folderId)
            } else {
                messageStore.getUnreadMessageCount(folderId)
            }
        } catch (e: Exception) {
            Log.e(e, "Unable to getUnreadMessageCount for account: %s, folder: %d", account, folderId)
            0
        }
    }

    override fun getMessageCountsFlow(search: LocalMessageSearch): Flow<MessageCounts> {
        return callbackFlow {
            send(getMessageCounts(search))

            val folderStatusChangedListener = object : SimpleMessagingListener() {
                override fun folderStatusChanged(account: LegacyAccountDto, folderId: Long) {
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
    private fun getMessageCounts(account: LegacyAccountDto, conditions: SearchConditionTreeNode?): MessageCounts {
        return try {
            val messageStore = messageStoreManager.getMessageStore(account)
            return MessageCounts(
                unread = messageStore.getUnreadMessageCount(conditions),
                starred = messageStore.getStarredMessageCount(conditions),
            )
        } catch (e: Exception) {
            Log.e(e, "Unable to getMessageCounts for account: %s", account)
            MessageCounts(unread = 0, starred = 0)
        }
    }
}
