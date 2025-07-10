package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UnifiedFolderRepository
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType

internal class GetDisplayFoldersForAccount(
    private val displayFolderRepository: DisplayFolderRepository,
    private val unifiedFolderRepository: UnifiedFolderRepository,
) : UseCase.GetDisplayFoldersForAccount {
    override fun invoke(accountId: String): Flow<List<DisplayFolder>> {
        if (accountId == UnifiedDisplayAccount.UNIFIED_ACCOUNT_ID) {
            return unifiedFolderRepository.getUnifiedDisplayFolderFlow(UnifiedDisplayFolderType.INBOX)
                .map { displayUnifiedFolder ->
                    listOf(displayUnifiedFolder)
                }
        } else {
            return displayFolderRepository.getDisplayFoldersFlow(accountId).map { displayFolders ->
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
        }
    }
}
