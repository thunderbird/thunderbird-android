package app.k9mail.legacy.ui.folder

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId

interface DisplayFolderRepository {

    fun getDisplayFoldersFlow(accountId: AccountId, includeHiddenFolders: Boolean): Flow<List<DisplayFolder>>

    fun getDisplayFoldersFlow(accountId: AccountId): Flow<List<DisplayFolder>>
}
