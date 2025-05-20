package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount

internal class FakeDisplayFolderRepository(
    private val foldersFlow: Flow<List<DisplayFolder>>,
) : DisplayFolderRepository {
    override fun getDisplayFoldersFlow(
        account: LegacyAccount,
        includeHiddenFolders: Boolean,
    ): Flow<List<DisplayFolder>> {
        TODO("Not yet implemented")
    }

    override fun getDisplayFoldersFlow(accountUuid: String): Flow<List<DisplayFolder>> {
        return foldersFlow
    }
}
