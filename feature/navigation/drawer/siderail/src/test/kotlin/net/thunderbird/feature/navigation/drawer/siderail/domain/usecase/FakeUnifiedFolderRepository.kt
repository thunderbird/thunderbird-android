package net.thunderbird.feature.navigation.drawer.siderail.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.navigation.drawer.siderail.domain.DomainContract.UnifiedFolderRepository
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayUnifiedFolder
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayUnifiedFolderType

internal class FakeUnifiedFolderRepository(
    private val displayUnifiedFolderFlow: Flow<DisplayUnifiedFolder>,
) : UnifiedFolderRepository {
    override fun getDisplayUnifiedFolderFlow(unifiedFolderType: DisplayUnifiedFolderType): Flow<DisplayUnifiedFolder> {
        return displayUnifiedFolderFlow
    }
}
