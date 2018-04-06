package com.fsck.k9.helper

import android.content.Context
import android.content.Intent
import com.fsck.k9.Account
import com.fsck.k9.BaseAccount
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.FolderList
import com.fsck.k9.activity.MessageList
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.UnreadWidgetProperties.Type.ACCOUNT
import com.fsck.k9.helper.UnreadWidgetProperties.Type.FOLDER
import com.fsck.k9.helper.UnreadWidgetProperties.Type.SEARCH_ACCOUNT
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount

class UnreadWidgetProperties(val appWidgetId: Int, val accountUuid: String, val folderServerId: String?) {
    private val type: Type = calculateType()


    fun getTitle(context: Context): String? {
        val accountName = getAccount(context).description
        return when (type) {
            SEARCH_ACCOUNT, ACCOUNT -> accountName
            FOLDER -> context.getString(R.string.unread_widget_title, accountName, folderServerId)
        }
    }

    @Throws(MessagingException::class)
    fun getUnreadCount(context: Context): Int {
        val controller = MessagingController.getInstance(context)
        val baseAccount = getAccount(context)

        return when (type) {
            SEARCH_ACCOUNT -> {
                val stats = controller.getSearchAccountStatsSynchronous(baseAccount as SearchAccount, null)
                stats.unreadMessageCount
            }
            ACCOUNT -> {
                val stats = controller.getAccountStats(baseAccount as Account)
                stats.unreadMessageCount
            }
            FOLDER -> controller.getFolderUnreadMessageCount(baseAccount as Account, folderServerId)
        }
    }

    fun getClickIntent(context: Context): Intent = when (type) {
        SEARCH_ACCOUNT -> {
            val searchAccount = getAccount(context) as SearchAccount
            MessageList.intentDisplaySearch(context,
                    searchAccount.relatedSearch, false, true, true)
        }
        ACCOUNT -> getClickIntentForAccount(context)
        FOLDER -> getClickIntentForFolder(context)
    }

    private fun calculateType(): Type {
        return if (SearchAccount.UNIFIED_INBOX == accountUuid || SearchAccount.ALL_MESSAGES == accountUuid) {
            SEARCH_ACCOUNT
        } else if (folderServerId != null) {
            FOLDER
        } else {
            ACCOUNT
        }
    }

    private fun getAccount(context: Context): BaseAccount = when (accountUuid) {
        SearchAccount.UNIFIED_INBOX -> SearchAccount.createUnifiedInboxAccount(context)
        SearchAccount.ALL_MESSAGES -> SearchAccount.createAllMessagesAccount(context)
        else -> Preferences.getPreferences(context).getAccount(accountUuid)
    }

    private fun getClickIntentForAccount(context: Context): Intent {
        val account = Preferences.getPreferences(context).getAccount(accountUuid)
        if (K9.FOLDER_NONE == account.autoExpandFolder) {
            return FolderList.actionHandleAccountIntent(context, account, false)
        }

        val search = LocalSearch(account.autoExpandFolder)
        search.addAllowedFolder(account.autoExpandFolder)
        search.addAccountUuid(account.uuid)
        return MessageList.intentDisplaySearch(context, search, false, true, true)
    }

    private fun getClickIntentForFolder(context: Context): Intent {
        val account = Preferences.getPreferences(context).getAccount(accountUuid)
        val search = LocalSearch(folderServerId)
        search.addAllowedFolder(folderServerId)
        search.addAccountUuid(account.uuid)

        val clickIntent = MessageList.intentDisplaySearch(context, search, false, true, true)
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        return clickIntent
    }


    enum class Type {
        SEARCH_ACCOUNT,
        ACCOUNT,
        FOLDER
    }
}
