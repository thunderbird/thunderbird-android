package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.flow.Flow

class GetDisplayFoldersForAccount(
    private val repository: DisplayFolderRepository,
) : UseCase.GetDisplayFoldersForAccount {
    override fun invoke(accountUuid: String): Flow<List<DisplayFolder>> {
        return repository.getDisplayFoldersFlow(accountUuid)
    }
}
