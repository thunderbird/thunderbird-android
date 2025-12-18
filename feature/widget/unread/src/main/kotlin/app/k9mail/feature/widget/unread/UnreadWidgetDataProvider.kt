package app.k9mail.feature.widget.unread

import android.content.Context
import android.content.Intent
import app.k9mail.legacy.mailstore.FolderRepository
import app.k9mail.legacy.message.controller.MessageCountsProvider
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MainActivity
import com.fsck.k9.ui.messagelist.DefaultFolderProvider
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount

private const val TAG = "UnreadWidgetDataProvider"

@Suppress("LongParameterList")
class UnreadWidgetDataProvider(
    private val context: Context,
    private val preferences: Preferences,
    private val messageCountsProvider: MessageCountsProvider,
    private val defaultFolderProvider: DefaultFolderProvider,
    private val folderRepository: FolderRepository,
    private val folderNameFormatter: FolderNameFormatter,
    private val coreResourceProvider: CoreResourceProvider,
    private val logger: Logger,
) {
    fun loadUnreadWidgetData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? = with(configuration) {
        if (SearchAccount.UNIFIED_FOLDERS == accountUuid) {
            loadUnifiedFoldersData(configuration)
        } else if (folderId != null) {
            loadFolderData(configuration)
        } else {
            loadAccountData(configuration)
        }
    }

    private fun loadUnifiedFoldersData(configuration: UnreadWidgetConfiguration): UnreadWidgetData {
        val searchAccount = getUnifiedFoldersSearch(configuration.accountUuid)
        val title = searchAccount.name
        val unreadCount = messageCountsProvider.getMessageCounts(searchAccount).unread
        val clickIntent = MainActivity.intentDisplaySearch(context, searchAccount.relatedSearch, false, true, true)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getUnifiedFoldersSearch(accountUuid: String): SearchAccount = when (accountUuid) {
        SearchAccount.UNIFIED_FOLDERS -> SearchAccount.createUnifiedFoldersSearch(
            title = coreResourceProvider.searchUnifiedFoldersTitle(),
            detail = coreResourceProvider.searchUnifiedFoldersDetail(),
        )
        else -> throw AssertionError("SearchAccount expected")
    }

    private fun loadAccountData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? {
        val account = preferences.getAccount(configuration.accountUuid) ?: return null
        val title = account.displayName
        val unreadCount = messageCountsProvider.getMessageCounts(account).unread
        val clickIntent = getClickIntentForAccount(account)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getClickIntentForAccount(account: LegacyAccountDto): Intent {
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
        val title = context.getString(R.string.unread_widget_title, accountName, folderDisplayName)

        val unreadCount = messageCountsProvider.getUnreadMessageCount(account, folderId)

        val clickIntent = getClickIntentForFolder(account, folderId)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getFolderDisplayName(account: LegacyAccountDto, folderId: Long): String {
        val folder = runBlocking { folderRepository.getFolder(account, folderId) }
        return if (folder != null) {
            folderNameFormatter.displayName(folder)
        } else {
            logger.error(TAG) { "Error loading folder for account ${account.id.asRaw()}, folder ID: $folderId" }
            ""
        }
    }

    private fun getClickIntentForFolder(account: LegacyAccountDto, folderId: Long): Intent {
        val search = LocalMessageSearch()
        search.addAllowedFolder(folderId)
        search.addAccountUuid(account.uuid)

        val clickIntent = MainActivity.intentDisplaySearch(context, search, false, true, true)
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        return clickIntent
    }
}
