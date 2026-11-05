package com.fsck.k9.ui.messagelist

import app.k9mail.legacy.mailstore.MessageListRepository
import com.fsck.k9.contacts.ContactLetterBitmapCreator
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.MessageColumns
import com.fsck.k9.search.conditionsFor
import com.fsck.k9.search.folderIdFor
import com.fsck.k9.search.getLegacyAccounts
import com.fsck.k9.search.unifiedFolderRole
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.SearchConditionTreeNode
import net.thunderbird.feature.search.legacy.sql.SqlWhereClause
import net.thunderbird.legacy.logging.Log

private const val TAG = "MessageListLoader"

@Suppress("LongParameterList")
class MessageListLoader(
    private val accountManager: LegacyAccountManager,
    private val localStoreProvider: LocalStoreProvider,
    private val messageListRepository: MessageListRepository,
    private val messageHelper: MessageHelper,
    private val messageListPreferencesManager: MessageListPreferencesManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val featureFlagProvider: FeatureFlagProvider,
    private val contactLetterBitmapCreator: ContactLetterBitmapCreator,
) {

    fun getMessageList(config: MessageListConfig): MessageListInfo {
        return try {
            getMessageListInfo(config)
        } catch (e: Exception) {
            Log.e(TAG, e, "Error while fetching message list")

            // TODO: Return an error object instead of an empty list
            MessageListInfo(
                messageListItems = emptyList(),
                hasMoreMessages = false,
                hasUnsyncedFolders = false,
                hasRemoteFolders = true,
            )
        }
    }

    private fun getMessageListInfo(config: MessageListConfig): MessageListInfo {
        val accounts = config.search.getLegacyAccounts(accountManager)
        val messageListItems = accounts
            .flatMap { account ->
                loadMessageListForAccount(account, config)
            }
            .sortedWith(config)

        val folders = loadFolders(accounts, config.search)
        // Local folders like the outbox have nothing more to download from the server.
        val singleFolder = folders?.singleOrNull()?.takeIf { config.search.folderIds.size == 1 }
        val hasMoreMessages = singleFolder != null && !singleFolder.isLocalOnly && singleFolder.hasMoreMessages()
        val hasUnsyncedFolders = messageListItems.isEmpty() &&
            folders.orEmpty().any { folder -> !folder.isLocalOnly && folder.lastChecked == 0L }
        val hasRemoteFolders = folders?.any { folder -> !folder.isLocalOnly } ?: true

        return MessageListInfo(messageListItems, hasMoreMessages, hasUnsyncedFolders, hasRemoteFolders)
    }

    /**
     * Loads the folders the message list shows, or returns `null` if it shows neither a single folder nor a unified
     * folder.
     */
    private fun loadFolders(accounts: List<LegacyAccount>, search: LocalMessageSearch): List<LocalFolder>? {
        val unifiedFolderRole = search.unifiedFolderRole
        val folderIds = when {
            unifiedFolderRole != null -> accounts.mapNotNull { account ->
                account.folderIdFor(unifiedFolderRole, outboxFolderManager)?.let { folderId -> account to folderId }
            }

            accounts.size == 1 && search.folderIds.size == 1 -> listOf(accounts[0] to search.folderIds[0])
            else -> return null
        }

        return folderIds.mapNotNull { (account, folderId) -> loadFolder(account, folderId) }
    }

    private fun loadFolder(account: LegacyAccount, folderId: Long): LocalFolder? {
        return try {
            localStoreProvider.getInstance(account.id).getFolder(folderId).apply { open() }
        } catch (e: MessagingException) {
            Log.w(TAG, e, "Couldn't load folder %d", folderId)
            null
        }
    }

    private fun loadMessageListForAccount(account: LegacyAccount, config: MessageListConfig): List<MessageListItem> {
        val accountId = account.id
        val conditions = config.search.conditionsFor(account, outboxFolderManager) ?: return emptyList()
        val threadId = getThreadId(config.search)
        val sortOrder = buildSortOrder(config)
        val mapper = MessageListItemMapper(
            messageHelper,
            account,
            messageListPreferencesManager,
            outboxFolderManager,
            contactLetterBitmapCreator = contactLetterBitmapCreator.takeIf {
                featureFlagProvider.provide(GeneratedFeatureFlagKey.USE_COMPOSE_FOR_MESSAGE_LIST_ITEMS).isEnabled() ||
                    featureFlagProvider.provide(GeneratedFeatureFlagKey.ENABLE_MESSAGE_LIST_NEW_STATE).isEnabled()
            },
        )

        return when {
            threadId != null -> {
                messageListRepository.getThread(accountId, threadId, sortOrder, mapper)
            }

            config.showingThreadedList -> {
                val (selection, selectionArgs) = buildSelection(account, config, conditions)
                messageListRepository.getThreadedMessages(accountId, selection, selectionArgs, sortOrder, mapper)
            }

            else -> {
                val (selection, selectionArgs) = buildSelection(account, config, conditions)
                messageListRepository.getMessages(accountId, selection, selectionArgs, sortOrder, mapper)
            }
        }
    }

    private fun buildSelection(
        account: LegacyAccount,
        config: MessageListConfig,
        conditions: SearchConditionTreeNode,
    ): Pair<String, Array<String>> {
        val query = StringBuilder()
        val queryArgs = mutableListOf<String>()

        val activeMessage = config.activeMessage
        val selectActive = activeMessage != null && activeMessage.accountId == account.id
        if (selectActive && activeMessage != null) {
            query.append("(${MessageColumns.UID} = ? AND ${MessageColumns.FOLDER_ID} = ?) OR (")
            queryArgs.add(activeMessage.uid)
            queryArgs.add(activeMessage.folderId.toString())
        }

        val whereClause = SqlWhereClause.Builder()
            .withConditions(conditions)
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

/**
 * The result of loading a message list.
 *
 * @property messageListItems The messages to show, sorted.
 * @property hasMoreMessages `true` if the folder has more messages on the server that can be loaded. Only set for a
 *   single folder that isn't local-only.
 * @property hasUnsyncedFolders `true` if the message list is empty and at least one of the folders it shows has never
 *   been synchronized with the server.
 * @property hasRemoteFolders `false` if all folders the message list shows are local-only, like the outbox, so there's
 *   nothing to synchronize.
 */
data class MessageListInfo(
    val messageListItems: List<MessageListItem>,
    val hasMoreMessages: Boolean,
    val hasUnsyncedFolders: Boolean,
    val hasRemoteFolders: Boolean,
)
