package app.k9mail.feature.widget.unread

import android.content.Context
import android.content.Intent
import app.k9mail.legacy.message.controller.MessageCountsProvider
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.activity.MessageHomeActivity
import com.fsck.k9.ui.messagelist.DefaultFolderProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount

private const val TAG = "UnreadWidgetDataProvider"

@Suppress("LongParameterList")
class UnreadWidgetDataProvider(
    private val context: Context,
    private val accountManager: LegacyAccountManager,
    private val messageCountsProvider: MessageCountsProvider,
    private val defaultFolderProvider: DefaultFolderProvider,
    private val folderQueryRepository: FolderQueryRepository,
    private val folderNameFormatter: FolderNameFormatter,
    private val coreResourceProvider: CoreResourceProvider,
    private val logger: Logger,
) {
    suspend fun loadUnreadWidgetData(
        configuration: UnreadWidgetConfiguration,
    ): UnreadWidgetData? = with(configuration) {
        if (UnifiedAccountId == accountId) {
            loadUnifiedFoldersData(configuration)
        } else if (folderId != null) {
            loadFolderData(configuration)
        } else {
            loadAccountData(configuration)
        }
    }

    private suspend fun loadUnifiedFoldersData(configuration: UnreadWidgetConfiguration): UnreadWidgetData {
        val searchAccount = getUnifiedFoldersSearch(configuration.accountId)
        val title = searchAccount.name
        val unreadCount = withContext(Dispatchers.IO) {
            messageCountsProvider.getMessageCounts(searchAccount).unread
        }
        val clickIntent = MessageHomeActivity.intentDisplaySearch(
            context,
            searchAccount.relatedSearch,
            false,
            true,
            true,
        )

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getUnifiedFoldersSearch(accountId: AccountId): SearchAccount = when (accountId) {
        UnifiedAccountId -> SearchAccount.createUnifiedInboxSearch(
            title = coreResourceProvider.searchUnifiedFoldersTitle(),
            detail = coreResourceProvider.searchUnifiedFoldersDetail(),
        )

        else -> throw AssertionError("SearchAccount expected")
    }

    @Suppress("ReturnCount")
    private suspend fun loadAccountData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? {
        val accountId = configuration.accountId
        val account = accountManager.findById(configuration.accountId) ?: return null
        val title = account.profile.name
        val unreadCount = withContext(Dispatchers.IO) {
            messageCountsProvider.getMessageCounts(accountId).unread
        }
        val clickIntent = getClickIntentForAccount(accountId)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private fun getClickIntentForAccount(accountId: AccountId): Intent {
        val folderId = defaultFolderProvider.getDefaultFolder(accountId)
        return getClickIntentForFolder(accountId, folderId)
    }

    @Suppress("ReturnCount")
    private suspend fun loadFolderData(configuration: UnreadWidgetConfiguration): UnreadWidgetData? {
        val accountId = configuration.accountId
        val account = accountManager.findById(accountId) ?: return null
        val folderId = configuration.folderId ?: return null

        val accountName = account.profile.name
        val folderDisplayName = getFolderDisplayName(account, folderId)
        val title = context.getString(R.string.unread_widget_title, accountName, folderDisplayName)

        val unreadCount = withContext(Dispatchers.IO) {
            messageCountsProvider.getUnreadMessageCount(accountId, folderId)
        }

        val clickIntent = getClickIntentForFolder(accountId, folderId)

        return UnreadWidgetData(configuration, title, unreadCount, clickIntent)
    }

    private suspend fun getFolderDisplayName(account: LegacyAccount, folderId: Long): String {
        val folder = folderQueryRepository.findById(account.id, folderId)
            .fold(
                onSuccess = { it },
                onFailure = { error ->
                    when (val throwable = error.throwable) {
                        null -> null
                        else -> throw throwable
                    }
                },
            )
        return if (folder != null) {
            folderNameFormatter.displayName(folder)
        } else {
            logger.error(TAG) { "Error loading type for account ${account.id}, type ID: $folderId" }
            ""
        }
    }

    private fun getClickIntentForFolder(accountId: AccountId, folderId: Long): Intent {
        val search = LocalMessageSearch()
        search.addAllowedFolder(folderId)
        search.addAccountId(accountId)

        val clickIntent = MessageHomeActivity.intentDisplaySearch(context, search, false, true, true)
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        return clickIntent
    }
}
