package net.thunderbird.feature.widget.message.list

import app.k9mail.legacy.mailstore.MessageListRepository
import com.fsck.k9.Preferences
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mailstore.MessageColumns
import com.fsck.k9.search.getAccounts
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.search.sql.SqlQueryBuilder

internal class MessageListLoader(
    private val preferences: Preferences,
    private val messageListRepository: MessageListRepository,
    private val messageHelper: MessageHelper,
    private val generalSettingsManager: GeneralSettingsManager,
) {

    @Suppress("TooGenericExceptionCaught")
    fun getMessageList(config: MessageListConfig): List<MessageListItem> {
        return try {
            getMessageListInfo(config)
        } catch (e: Exception) {
            Log.e(e, "Error while fetching message list")

            // TODO: Return an error object instead of an empty list
            emptyList()
        }
    }

    private fun getMessageListInfo(config: MessageListConfig): List<MessageListItem> {
        val accounts = config.search.getAccounts(preferences)
        val messageListItems = accounts
            .flatMap { account ->
                loadMessageListForAccount(account, config)
            }
            .sortedWith(config)

        return messageListItems
    }

    private fun loadMessageListForAccount(account: LegacyAccount, config: MessageListConfig): List<MessageListItem> {
        val accountUuid = account.uuid
        val sortOrder = buildSortOrder(config)
        val mapper = MessageListItemMapper(messageHelper, account, generalSettingsManager)

        return if (config.showingThreadedList) {
            val (selection, selectionArgs) = buildSelection(config)
            messageListRepository.getThreadedMessages(accountUuid, selection, selectionArgs, sortOrder, mapper)
        } else {
            val (selection, selectionArgs) = buildSelection(config)
            messageListRepository.getMessages(accountUuid, selection, selectionArgs, sortOrder, mapper)
        }
    }

    private fun buildSelection(config: MessageListConfig): Pair<String, Array<String>> {
        val query = StringBuilder()
        val queryArgs = mutableListOf<String>()

        SqlQueryBuilder.buildWhereClause(config.search.conditions, query, queryArgs)

        val selection = query.toString()
        val selectionArgs = queryArgs.toTypedArray()

        return selection to selectionArgs
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
                compareBy(config.sortAscending) { it.sortMessageDate }
            }

            SortType.SORT_ARRIVAL -> {
                compareBy(config.sortAscending) { it.sortInternalDate }
            }

            SortType.SORT_SUBJECT -> {
                compareStringBy<MessageListItem>(config.sortAscending) { it.sortSubject.orEmpty() }
                    .thenByDate(config)
            }

            SortType.SORT_SENDER -> {
                compareStringBy<MessageListItem>(config.sortAscending) { it.displayName }
                    .thenByDate(config)
            }

            SortType.SORT_UNREAD -> {
                compareBy<MessageListItem>(config.sortAscending) { it.isRead }
                    .thenByDate(config)
            }

            SortType.SORT_FLAGGED -> {
                compareBy<MessageListItem>(!config.sortAscending) { it.sortIsStarred }
                    .thenByDate(config)
            }

            SortType.SORT_ATTACHMENT -> {
                compareBy<MessageListItem>(!config.sortAscending) { it.hasAttachments }
                    .thenByDate(config)
            }
        }.thenByDescending { it.sortDatabaseId }

        return this.sortedWith(comparator)
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
        thenBy { it.sortMessageDate }
    } else {
        thenByDescending { it.sortMessageDate }
    }
}
