package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetDisplayFoldersForAccount(
    private val repository: DisplayFolderRepository,
) : UseCase.GetDisplayFoldersForAccount {
    override fun invoke(accountUuid: String): Flow<List<DisplayAccountFolder>> {
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
        }
    }
}
