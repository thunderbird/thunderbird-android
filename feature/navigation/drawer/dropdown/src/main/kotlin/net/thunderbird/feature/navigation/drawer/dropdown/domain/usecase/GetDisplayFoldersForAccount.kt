package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UnifiedFolderRepository
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType

internal class GetDisplayFoldersForAccount(
    private val displayFolderRepository: DisplayFolderRepository,
    private val unifiedFolderRepository: UnifiedFolderRepository,
) : UseCase.GetDisplayFoldersForAccount {
    override fun invoke(accountId: String, includeUnifiedFolders: Boolean): Flow<List<DisplayFolder>> {
        val accountFoldersFlow: Flow<List<DisplayFolder>> =
            displayFolderRepository.getDisplayFoldersFlow(accountId).map { displayFolders ->
                displayFolders.map { displayFolder ->
                    MailDisplayFolder(
                        accountId = accountId,
                        folder = displayFolder.folder,
                        isInTopGroup = displayFolder.isInTopGroup,
                        unreadMessageCount = displayFolder.unreadMessageCount,
                        starredMessageCount = displayFolder.starredMessageCount,
                    )
                }
            }

        val unifiedFoldersFlow: Flow<List<DisplayFolder>> = if (includeUnifiedFolders) {
            unifiedFolderRepository.getDisplayUnifiedFolderFlow(UnifiedDisplayFolderType.INBOX)
                .map { displayUnifiedFolder ->
                    listOf(displayUnifiedFolder)
                }
        } else {
            flowOf(emptyList<UnifiedDisplayFolder>())
        }

        return combine(
            accountFoldersFlow,
            unifiedFoldersFlow,
        ) { accountFolders, unifiedFolders ->
            unifiedFolders + accountFolders
        }
    }
}
