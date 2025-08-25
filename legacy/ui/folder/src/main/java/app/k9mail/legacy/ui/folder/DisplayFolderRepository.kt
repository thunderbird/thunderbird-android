package app.k9mail.legacy.ui.folder

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount

interface DisplayFolderRepository {

    fun getDisplayFoldersFlow(account: LegacyAccount, includeHiddenFolders: Boolean): Flow<List<DisplayFolder>>

    fun getDisplayFoldersFlow(accountUuid: String): Flow<List<DisplayFolder>>
}
