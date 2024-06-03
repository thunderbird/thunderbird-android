package app.k9mail.feature.widget.unread

import android.content.Context
import android.content.Intent
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageList
import com.fsck.k9.controller.MessageCountsProvider
import com.fsck.k9.mailstore.FolderRepository
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.messagelist.DefaultFolderProvider
import timber.log.Timber
import com.fsck.k9.ui.R as UiR

class UnreadWidgetDataProvider(
    private val context: Context,
    private val preferences: Preferences,
    private val messageCountsProvider: MessageCountsProvider,
    private val defaultFolderProvider: DefaultFolderProvider,
    private val folderRepository: FolderRepository,
    private val folderNameFormatter: FolderNameFormatter,
) {
    fun loadUnreadWidgetData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? = with(configuration) {
        if (SearchAccount.UNIFIED_INBOX == accountUuid) {
            loadSearchAccountData(configuration)
        } else if (folderId != null) {
            loadFolderData(configuration)
        } else {
            loadAccountData(configuration)
        }
    }

    private fun loadSearchAccountData(configuration: UnreadWidgetConfiguration): UnreadWidgetData {
        val searchAccount = getSearchAccount(configuration.accountUuid)
        val title = searchAccount.name
        val unreadCount = messageCountsProvider.getMessageCounts(searchAccount).unread
        val clickIntent = MessageList.intentDisplaySearch(context, searchAccount.relatedSearch, false, true, true)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getSearchAccount(accountUuid: String): SearchAccount = when (accountUuid) {
        SearchAccount.UNIFIED_INBOX -> SearchAccount.createUnifiedInboxAccount()
        else -> throw AssertionError("SearchAccount expected")
    }

    private fun loadAccountData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? {
        val account = preferences.getAccount(configuration.accountUuid) ?: return null
        val title = account.displayName
        val unreadCount = messageCountsProvider.getMessageCounts(account).unread
        val clickIntent = getClickIntentForAccount(account)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getClickIntentForAccount(account: Account): Intent {
        val folderId = defaultFolderProvider.getDefaultFolder(account)
        return getClickIntentForFolder(account, folderId)
    }

    @Suppress("ReturnCount")
    private fun loadFolderData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? {
        val accountUuid = configuration.accountUuid
        val account = preferences.getAccount(accountUuid) ?: return null
        val folderId = configuration.folderId ?: return null

        val accountName = account.displayName
        val folderDisplayName = getFolderDisplayName(account, folderId)
        val title = context.getString(UiR.string.unread_widget_title, accountName, folderDisplayName)

        val unreadCount = messageCountsProvider.getUnreadMessageCount(account, folderId)

        val clickIntent = getClickIntentForFolder(account, folderId)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getFolderDisplayName(account: Account, folderId: Long): String {
        val folder = folderRepository.getFolder(account, folderId)
        return if (folder != null) {
            folderNameFormatter.displayName(folder)
        } else {
            Timber.e("Error loading folder for account %s, folder ID: %d", account, folderId)
            ""
        }
    }

    private fun getClickIntentForFolder(account: Account, folderId: Long): Intent {
        val search = LocalSearch()
        search.addAllowedFolder(folderId)
        search.addAccountUuid(account.uuid)

        val clickIntent = MessageList.intentDisplaySearch(context, search, false, true, true)
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        return clickIntent
    }
}
