package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UnifiedFolderRepository
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayUnifiedFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayUnifiedFolderType

internal class FakeUnifiedFolderRepository(
    private val displayUnifiedFolderFlow: Flow<DisplayUnifiedFolder>,
) : UnifiedFolderRepository {
    override fun getDisplayUnifiedFolderFlow(unifiedFolderType: DisplayUnifiedFolderType): Flow<DisplayUnifiedFolder> {
        return displayUnifiedFolderFlow
    }
}
