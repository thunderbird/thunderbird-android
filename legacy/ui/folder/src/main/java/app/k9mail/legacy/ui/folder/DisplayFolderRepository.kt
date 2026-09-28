package app.k9mail.legacy.ui.folder

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.AccountId

interface DisplayFolderRepository {

    fun getDisplayFoldersFlow(account: LegacyAccountDto, includeHiddenFolders: Boolean): Flow<List<DisplayFolder>>

    fun getDisplayFoldersFlow(accountId: String): Flow<List<DisplayFolder>>
}
