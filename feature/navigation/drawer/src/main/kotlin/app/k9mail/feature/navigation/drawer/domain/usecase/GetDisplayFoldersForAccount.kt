package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolderType
import app.k9mail.legacy.message.controller.MessageCountsProvider
import app.k9mail.legacy.search.LocalSearch
import app.k9mail.legacy.search.api.SearchAttribute
import app.k9mail.legacy.search.api.SearchField
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GetDisplayFoldersForAccount(
    private val repository: DisplayFolderRepository,
    private val messageCountsProvider: MessageCountsProvider,
) : UseCase.GetDisplayFoldersForAccount {
    override fun invoke(accountUuid: String, includeUnifiedFolders: Boolean): Flow<List<DisplayFolder>> {
        return repository.getDisplayFoldersFlow(accountUuid).map { displayFolders ->
            displayFolders.map { displayFolder ->
                DisplayAccountFolder(
                    accountUuid = accountUuid,
                    folder = displayFolder.folder,
                    isInTopGroup = displayFolder.isInTopGroup,
                    unreadMessageCount = displayFolder.unreadMessageCount,
                    starredMessageCount = displayFolder.starredMessageCount,
                )
            }
        }.map { displayFolders ->
            if (includeUnifiedFolders) {
                createDisplayUnifiedFolders() + displayFolders
            } else {
                displayFolders
            }
        }
    }

    private fun createDisplayUnifiedFolders(): List<DisplayUnifiedFolder> {
        return listOf(
            createUnifiedInboxFolder(),
        )
    }

    private fun createUnifiedInboxFolder(): DisplayUnifiedFolder {
        val search = getUnifiedInboxSearch()
        val messageCounts = messageCountsProvider.getMessageCounts(search)

        return DisplayUnifiedFolder(
            id = UNIFIED_INBOX_ID,
            unifiedType = DisplayUnifiedFolderType.INBOX,
            unreadMessageCount = messageCounts.unread,
            starredMessageCount = messageCounts.starred,
        )
    }

    private fun getUnifiedInboxSearch(): LocalSearch {
        return LocalSearch().apply {
            id = UNIFIED_INBOX_ID
            and(SearchField.INTEGRATE, "1", SearchAttribute.EQUALS)
        }
    }

    companion object {
        private const val UNIFIED_INBOX_ID = "unified_inbox"
    }
}
