package app.k9mail.legacy.ui.folder

import app.k9mail.legacy.account.LegacyAccount
import kotlinx.coroutines.flow.Flow

interface DisplayFolderRepository {

    fun getDisplayFoldersFlow(account: LegacyAccount, includeHiddenFolders: Boolean): Flow<List<DisplayFolder>>

    fun getDisplayFoldersFlow(accountUuid: String): Flow<List<DisplayFolder>>
}
