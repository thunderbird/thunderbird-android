package net.thunderbird.feature.navigation.drawer.dropdown.data

import app.k9mail.legacy.message.controller.MessageCountsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.search.legacy.LocalMessageSearch

internal class UnifiedFolderRepository(
    private val messageCountsProvider: MessageCountsProvider,
) : DomainContract.UnifiedFolderRepository {

    override fun getUnifiedDisplayFolderFlow(role: FolderRole): Flow<UnifiedDisplayFolder> {
        val search = LocalMessageSearch.createUnifiedSearch(role)

        return messageCountsProvider.getMessageCountsFlow(search).map {
            UnifiedDisplayFolder(
                role = role,
                unreadMessageCount = it.unread,
                starredMessageCount = it.starred,
            )
        }
    }
}
