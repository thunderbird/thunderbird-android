package com.fsck.k9.ui.messagelist

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import com.fsck.k9.Account
import com.fsck.k9.Account.SortType
import com.fsck.k9.Preferences
import com.fsck.k9.fragment.MLFProjectionInfo
import com.fsck.k9.fragment.MessageListFragmentComparators.ArrivalComparator
import com.fsck.k9.fragment.MessageListFragmentComparators.AttachmentComparator
import com.fsck.k9.fragment.MessageListFragmentComparators.ComparatorChain
import com.fsck.k9.fragment.MessageListFragmentComparators.DateComparator
import com.fsck.k9.fragment.MessageListFragmentComparators.FlaggedComparator
import com.fsck.k9.fragment.MessageListFragmentComparators.ReverseComparator
import com.fsck.k9.fragment.MessageListFragmentComparators.ReverseIdComparator
import com.fsck.k9.fragment.MessageListFragmentComparators.SenderComparator
import com.fsck.k9.fragment.MessageListFragmentComparators.SubjectComparator
import com.fsck.k9.fragment.MessageListFragmentComparators.UnreadComparator
import com.fsck.k9.helper.MergeCursorWithUniqueId
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.provider.EmailProvider
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchSpecification.SearchField
import com.fsck.k9.search.SqlQueryBuilder
import com.fsck.k9.search.getAccounts
import java.util.ArrayList
import java.util.Comparator

class MessageListLoader(
    private val preferences: Preferences,
    private val contentResolver: ContentResolver,
    private val localStoreProvider: LocalStoreProvider,
    private val messageListExtractor: MessageListExtractor
) {

    fun getMessageList(config: MessageListConfig): MessageListInfo {
        val accounts = config.search.getAccounts(preferences)
        val cursors = accounts
            .mapNotNull { loadMessageListForAccount(it, config) }
            .toTypedArray()

        val cursor: Cursor
        val uniqueIdColumn: Int
        if (cursors.size > 1) {
            cursor = MergeCursorWithUniqueId(cursors, getComparator(config))
            uniqueIdColumn = cursor.getColumnIndex("_id")
        } else {
            cursor = cursors[0]
            uniqueIdColumn = MLFProjectionInfo.ID_COLUMN
        }

        val messageListItems = cursor.use {
            messageListExtractor.extractMessageList(
                cursor,
                uniqueIdColumn,
                threadCountIncluded = config.showingThreadedList
            )
        }
        val hasMoreMessages = loadHasMoreMessages(accounts, config.search.folderIds)

        return MessageListInfo(messageListItems, hasMoreMessages)
    }

    private fun loadMessageListForAccount(account: Account, config: MessageListConfig): Cursor? {
        val accountUuid = account.uuid
        val threadId: String? = getThreadId(config.search)

        val uri: Uri
        val projection: Array<String>
        val needConditions: Boolean
        when {
            threadId != null -> {
                uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/$accountUuid/thread/$threadId")
                projection = MLFProjectionInfo.PROJECTION
                needConditions = false
            }
            config.showingThreadedList -> {
                uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/$accountUuid/messages/threaded")
                projection = MLFProjectionInfo.THREADED_PROJECTION
                needConditions = true
            }
            else -> {
                uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/$accountUuid/messages")
                projection = MLFProjectionInfo.PROJECTION
                needConditions = true
            }
        }

        val query = StringBuilder()
        val queryArgs: MutableList<String> = ArrayList()
        if (needConditions) {
            val activeMessage = config.activeMessage
            val selectActive = activeMessage != null && activeMessage.accountUuid == accountUuid
            if (selectActive && activeMessage != null) {
                query.append("(${EmailProvider.MessageColumns.UID} = ? AND ${EmailProvider.MessageColumns.FOLDER_ID} = ?) OR (")
                queryArgs.add(activeMessage.uid)
                queryArgs.add(activeMessage.folderId.toString())
            }

            SqlQueryBuilder.buildWhereClause(account, config.search.conditions, query, queryArgs)

            if (selectActive) {
                query.append(')')
            }
        }

        val selection = query.toString()
        val selectionArgs = queryArgs.toTypedArray()

        val sortOrder: String = buildSortOrder(config)

        return contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
    }

    private fun getThreadId(search: LocalSearch): String? {
        return search.leafSet.firstOrNull { it.condition.field == SearchField.THREAD_ID }?.condition?.value
    }

    private fun buildSortOrder(config: MessageListConfig): String {
        val sortColumn = when (config.sortType) {
            SortType.SORT_ARRIVAL -> EmailProvider.MessageColumns.INTERNAL_DATE
            SortType.SORT_ATTACHMENT -> "(${EmailProvider.MessageColumns.ATTACHMENT_COUNT} < 1)"
            SortType.SORT_FLAGGED -> "(${EmailProvider.MessageColumns.FLAGGED} != 1)"
            SortType.SORT_SENDER -> EmailProvider.MessageColumns.SENDER_LIST // FIXME
            SortType.SORT_SUBJECT -> "${EmailProvider.MessageColumns.SUBJECT} COLLATE NOCASE"
            SortType.SORT_UNREAD -> EmailProvider.MessageColumns.READ
            SortType.SORT_DATE -> EmailProvider.MessageColumns.DATE
            else -> EmailProvider.MessageColumns.DATE
        }

        val sortDirection = if (config.sortAscending) " ASC" else " DESC"
        val secondarySort = if (config.sortType == SortType.SORT_DATE || config.sortType == SortType.SORT_ARRIVAL) {
            ""
        } else {
            if (config.sortDateAscending) {
                "${EmailProvider.MessageColumns.DATE} ASC, "
            } else {
                "${EmailProvider.MessageColumns.DATE} DESC, "
            }
        }

        return "$sortColumn$sortDirection, $secondarySort${EmailProvider.MessageColumns.ID} DESC"
    }

    private fun getComparator(config: MessageListConfig): Comparator<Cursor>? {
        val chain: MutableList<Comparator<Cursor>> = ArrayList(3 /* we add 3 comparators at most */)

        // Add the specified comparator
        val comparator = SORT_COMPARATORS.getValue(config.sortType)
        if (config.sortAscending) {
            chain.add(comparator)
        } else {
            chain.add(ReverseComparator(comparator))
        }

        // Add the date comparator if not already specified
        if (config.sortType != SortType.SORT_DATE && config.sortType != SortType.SORT_ARRIVAL) {
            val dateComparator = SORT_COMPARATORS.getValue(SortType.SORT_DATE)
            if (config.sortDateAscending) {
                chain.add(dateComparator)
            } else {
                chain.add(ReverseComparator(dateComparator))
            }
        }

        // Add the id comparator
        chain.add(ReverseIdComparator())

        // Build the comparator chain
        return ComparatorChain(chain)
    }

    private fun loadHasMoreMessages(accounts: List<Account>, folderIds: List<Long>): Boolean {
        return if (accounts.size == 1 && folderIds.size == 1) {
            val account = accounts[0]
            val folderId = folderIds[0]
            val localStore = localStoreProvider.getInstance(account)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()
            localFolder.hasMoreMessages()
        } else {
            false
        }
    }

    companion object {
        private val SORT_COMPARATORS = mapOf(
            SortType.SORT_ATTACHMENT to AttachmentComparator(),
            SortType.SORT_DATE to DateComparator(),
            SortType.SORT_ARRIVAL to ArrivalComparator(),
            SortType.SORT_FLAGGED to FlaggedComparator(),
            SortType.SORT_SUBJECT to SubjectComparator(),
            SortType.SORT_SENDER to SenderComparator(),
            SortType.SORT_UNREAD to UnreadComparator()
        )
    }
}

data class MessageListInfo(val messageListItems: List<MessageListItem>, val hasMoreMessages: Boolean)
