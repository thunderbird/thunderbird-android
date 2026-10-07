package net.thunderbird.feature.navigation.drawer.dropdown.data

import app.k9mail.legacy.message.controller.MessageCountsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.LocalMessageSearchType
import net.thunderbird.feature.search.legacy.UnifiedFolderSelection
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute

internal class UnifiedFolderRepository(
    private val messageCountsProvider: MessageCountsProvider,
) : DomainContract.UnifiedFolderRepository {

    override fun getUnifiedDisplayFolderFlow(unifiedFolderType: UnifiedDisplayFolderType): Flow<UnifiedDisplayFolder> {
        return messageCountsProvider.getMessageCountsFlow(createUnifiedFolderSearch(unifiedFolderType)).map {
            UnifiedDisplayFolder(
                folderId = unifiedFolderType.id,
                unifiedType = unifiedFolderType,
                unreadMessageCount = it.unread,
                starredMessageCount = it.starred,
            )
        }
    }

    private fun createUnifiedFolderSearch(unifiedFolderType: UnifiedDisplayFolderType): LocalMessageSearch {
        return when (unifiedFolderType) {
            UnifiedDisplayFolderType.INBOX -> createUnifiedInboxSearch()
        }
    }

    private fun createUnifiedInboxSearch(): LocalMessageSearch {
        return LocalMessageSearch().apply {
            id = UnifiedAccountId
            type = LocalMessageSearchType.Unified(UnifiedFolderSelection.Special(FolderType.INBOX))
            and(MessageSearchField.INTEGRATE, "1", SearchAttribute.EQUALS)
        }
    }
}
