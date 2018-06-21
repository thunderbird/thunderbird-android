package com.fsck.k9.widget.unread

import android.content.Context
import android.content.Intent
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.core.R
import com.fsck.k9.activity.FolderList
import com.fsck.k9.activity.MessageList
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount

class UnreadWidgetDataProvider(
        private val context: Context,
        private val preferences: Preferences,
        private val messagingController: MessagingController
) {
    fun loadUnreadWidgetData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? = with(configuration) {
        if (SearchAccount.UNIFIED_INBOX == accountUuid || SearchAccount.ALL_MESSAGES == accountUuid) {
            loadSearchAccountData(configuration)
        } else if (folderServerId != null) {
            loadFolderData(configuration)
        } else {
            loadAccountData(configuration)
        }
    }

    private fun loadSearchAccountData(configuration: UnreadWidgetConfiguration): UnreadWidgetData {
        val searchAccount = getSearchAccount(configuration.accountUuid)
        val title = searchAccount.description

        val stats = messagingController.getSearchAccountStatsSynchronous(searchAccount, null)
        val unreadCount = stats.unreadMessageCount

        val clickIntent = MessageList.intentDisplaySearch(context, searchAccount.relatedSearch, false, true, true)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getSearchAccount(accountUuid: String): SearchAccount = when (accountUuid) {
        SearchAccount.UNIFIED_INBOX -> SearchAccount.createUnifiedInboxAccount(context)
        SearchAccount.ALL_MESSAGES -> SearchAccount.createAllMessagesAccount(context)
        else -> throw AssertionError("SearchAccount expected")
    }

    private fun loadAccountData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? {
        val account = preferences.getAccount(configuration.accountUuid) ?: return null

        val title = account.description

        val stats = messagingController.getAccountStats(account)
        val unreadCount = stats.unreadMessageCount

        val clickIntent = getClickIntentForAccount(account)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getClickIntentForAccount(account: Account): Intent {
        if (K9.FOLDER_NONE == account.autoExpandFolder) {
            return FolderList.actionHandleAccountIntent(context, account, false)
        }

        val search = LocalSearch(account.autoExpandFolder)
        search.addAllowedFolder(account.autoExpandFolder)
        search.addAccountUuid(account.uuid)
        return MessageList.intentDisplaySearch(context, search, false, true, true)
    }

    private fun loadFolderData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? {
        val accountUuid = configuration.accountUuid
        val account = preferences.getAccount(accountUuid) ?: return null
        val folderServerId = configuration.folderServerId ?: return null

        val accountName = account.description
        //FIXME: Use folder display name instead of folderServerId for title
        val title = context.getString(R.string.unread_widget_title, accountName, folderServerId)

        val unreadCount = messagingController.getFolderUnreadMessageCount(account, folderServerId)

        val clickIntent = getClickIntentForFolder(accountUuid, folderServerId)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getClickIntentForFolder(accountUuid: String, folderServerId: String): Intent {
        val account = preferences.getAccount(accountUuid)
        val search = LocalSearch(folderServerId)
        search.addAllowedFolder(folderServerId)
        search.addAccountUuid(account.uuid)

        val clickIntent = MessageList.intentDisplaySearch(context, search, false, true, true)
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        return clickIntent
    }
}
