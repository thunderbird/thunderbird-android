package com.fsck.k9.ui.messagelist

import app.k9mail.legacy.mailstore.MessageListRepository
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.MessageColumns
import com.fsck.k9.search.getLegacyAccounts
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.sql.SqlWhereClause

class MessageListLoader(
    private val accountManager: LegacyAccountManager,
    private val localStoreProvider: LocalStoreProvider,
    private val messageListRepository: MessageListRepository,
    private val messageHelper: MessageHelper,
    private val generalSettingsManager: GeneralSettingsManager,
    private val outboxFolderManager: OutboxFolderManager,
) {

    fun getMessageList(config: MessageListConfig): MessageListInfo {
        return try {
            getMessageListInfo(config)
        } catch (e: Exception) {
            Log.e(e, "Error while fetching message list")

            // TODO: Return an error object instead of an empty list
            MessageListInfo(messageListItems = emptyList(), hasMoreMessages = false)
        }
    }

    private fun getMessageListInfo(config: MessageListConfig): MessageListInfo {
        val accounts = config.search.getLegacyAccounts(accountManager)
        val messageListItems = accounts
            .flatMap { account ->
                loadMessageListForAccount(account, config)
            }
            .sortedWith(config)

        val hasMoreMessages = loadHasMoreMessages(accounts, config.search.folderIds)

        return MessageListInfo(messageListItems, hasMoreMessages)
    }

    private fun loadMessageListForAccount(account: LegacyAccount, config: MessageListConfig): List<MessageListItem> {
        val accountUuid = account.uuid
        val threadId = getThreadId(config.search)
        val sortOrder = buildSortOrder(config)
        val mapper = MessageListItemMapper(messageHelper, account, generalSettingsManager, outboxFolderManager)

        return when {
            threadId != null -> {
                messageListRepository.getThread(accountUuid, threadId, sortOrder, mapper)
            }

            config.showingThreadedList -> {
                val (selection, selectionArgs) = buildSelection(account, config)
                messageListRepository.getThreadedMessages(accountUuid, selection, selectionArgs, sortOrder, mapper)
            }

            else -> {
                val (selection, selectionArgs) = buildSelection(account, config)
                messageListRepository.getMessages(accountUuid, selection, selectionArgs, sortOrder, mapper)
            }
        }
    }

    private fun buildSelection(account: LegacyAccount, config: MessageListConfig): Pair<String, Array<String>> {
        val query = StringBuilder()
        val queryArgs = mutableListOf<String>()

        val activeMessage = config.activeMessage
        val selectActive = activeMessage != null && activeMessage.accountUuid == account.uuid
        if (selectActive && activeMessage != null) {
            query.append("(${MessageColumns.UID} = ? AND ${MessageColumns.FOLDER_ID} = ?) OR (")
            queryArgs.add(activeMessage.uid)
            queryArgs.add(activeMessage.folderId.toString())
        }

        val whereClause = SqlWhereClause.Builder()
            .withConditions(config.search.conditions)
            .build()

        query.append(whereClause.selection)
        queryArgs.addAll(whereClause.selectionArgs)

        if (selectActive) {
            query.append(')')
        }

        val selection = query.toString()
        val selectionArgs = queryArgs.toTypedArray()

        return selection to selectionArgs
    }

    private fun getThreadId(search: LocalMessageSearch): Long? {
        return search.leafSet.firstOrNull {
            it.condition?.field == MessageSearchField.THREAD_ID
        }?.condition?.value?.toLong()
    }

    private fun buildSortOrder(config: MessageListConfig): String {
        val sortColumn = when (config.sortType) {
            SortType.SORT_ARRIVAL -> MessageColumns.INTERNAL_DATE
            SortType.SORT_ATTACHMENT -> "(${MessageColumns.ATTACHMENT_COUNT} < 1)"
            SortType.SORT_FLAGGED -> "(${MessageColumns.FLAGGED} != 1)"
            SortType.SORT_SENDER -> MessageColumns.SENDER_LIST // FIXME
            SortType.SORT_SUBJECT -> "${MessageColumns.SUBJECT} COLLATE NOCASE"
            SortType.SORT_UNREAD -> MessageColumns.READ
            SortType.SORT_DATE -> MessageColumns.DATE
        }

        val sortDirection = if (config.sortAscending) " ASC" else " DESC"
        val secondarySort = if (config.sortType == SortType.SORT_DATE || config.sortType == SortType.SORT_ARRIVAL) {
            ""
        } else {
            if (config.sortDateAscending) {
                "${MessageColumns.DATE} ASC, "
            } else {
                "${MessageColumns.DATE} DESC, "
            }
        }

        return "$sortColumn$sortDirection, $secondarySort${MessageColumns.ID} DESC"
    }

    private fun List<MessageListItem>.sortedWith(config: MessageListConfig): List<MessageListItem> {
        val comparator = when (config.sortType) {
            SortType.SORT_DATE -> {
                compareBy(config.sortAscending) { it.messageDate }
            }

            SortType.SORT_ARRIVAL -> {
                compareBy(config.sortAscending) { it.internalDate }
            }

            SortType.SORT_SUBJECT -> {
                compareStringBy<MessageListItem>(config.sortAscending) { it.subject.orEmpty() }
                    .thenByDate(config)
            }

            SortType.SORT_SENDER -> {
                compareStringBy<MessageListItem>(config.sortAscending) { it.displayName.toString() }
                    .thenByDate(config)
            }

            SortType.SORT_UNREAD -> {
                compareBy<MessageListItem>(config.sortAscending) {
                    config.sortOverrides[it.messageReference]?.isRead ?: it.isRead
                }.thenByDate(config)
            }

            SortType.SORT_FLAGGED -> {
                compareBy<MessageListItem>(!config.sortAscending) {
                    config.sortOverrides[it.messageReference]?.isStarred ?: it.isStarred
                }.thenByDate(config)
            }

            SortType.SORT_ATTACHMENT -> {
                compareBy<MessageListItem>(!config.sortAscending) { it.hasAttachments }
                    .thenByDate(config)
            }
        }.thenByDescending { it.databaseId }

        return this.sortedWith(comparator)
    }

    private fun loadHasMoreMessages(accounts: List<LegacyAccount>, folderIds: List<Long>): Boolean {
        return if (accounts.size == 1 && folderIds.size == 1) {
            val account = accounts[0]
            val folderId = folderIds[0]
            val localStore = localStoreProvider.getInstanceByLegacyAccount(account)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()
            localFolder.hasMoreMessages()
        } else {
            false
        }
    }
}

private inline fun <T> compareBy(sortAscending: Boolean, crossinline selector: (T) -> Comparable<*>?): Comparator<T> {
    return if (sortAscending) {
        compareBy(selector)
    } else {
        compareByDescending(selector)
    }
}

private inline fun <T> compareStringBy(sortAscending: Boolean, crossinline selector: (T) -> String): Comparator<T> {
    return if (sortAscending) {
        compareBy(String.CASE_INSENSITIVE_ORDER, selector)
    } else {
        compareByDescending(String.CASE_INSENSITIVE_ORDER, selector)
    }
}

private fun Comparator<MessageListItem>.thenByDate(config: MessageListConfig): Comparator<MessageListItem> {
    return if (config.sortDateAscending) {
        thenBy { it.messageDate }
    } else {
        thenByDescending { it.messageDate }
    }
}

data class MessageListInfo(val messageListItems: List<MessageListItem>, val hasMoreMessages: Boolean)
