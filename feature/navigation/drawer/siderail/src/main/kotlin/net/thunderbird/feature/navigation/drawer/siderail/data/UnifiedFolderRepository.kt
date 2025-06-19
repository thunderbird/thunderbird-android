package net.thunderbird.feature.navigation.drawer.siderail.data

import app.k9mail.legacy.message.controller.MessageCountsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.feature.navigation.drawer.siderail.domain.DomainContract
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayUnifiedFolder
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayUnifiedFolderType
import net.thunderbird.feature.search.LocalMessageSearch
import net.thunderbird.feature.search.api.SearchAttribute
import net.thunderbird.feature.search.api.SearchField

internal class UnifiedFolderRepository(
    private val messageCountsProvider: MessageCountsProvider,
) : DomainContract.UnifiedFolderRepository {

    override fun getDisplayUnifiedFolderFlow(unifiedFolderType: DisplayUnifiedFolderType): Flow<DisplayUnifiedFolder> {
        return messageCountsProvider.getMessageCountsFlow(createUnifiedFolderSearch(unifiedFolderType)).map {
            DisplayUnifiedFolder(
                id = UNIFIED_INBOX_ID,
                unifiedType = DisplayUnifiedFolderType.INBOX,
                unreadMessageCount = it.unread,
                starredMessageCount = it.starred,
            )
        }
    }

    private fun createUnifiedFolderSearch(unifiedFolderType: DisplayUnifiedFolderType): LocalMessageSearch {
        return when (unifiedFolderType) {
            DisplayUnifiedFolderType.INBOX -> return createUnifiedInboxSearch()
        }
    }

    private fun createUnifiedInboxSearch(): LocalMessageSearch {
        return LocalMessageSearch().apply {
            id = UNIFIED_INBOX_ID
            and(SearchField.INTEGRATE, "1", SearchAttribute.EQUALS)
        }
    }

    companion object {
        const val UNIFIED_INBOX_ID = "unified_inbox"
    }
}
