package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UnifiedFolderRepository
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder

internal class FakeUnifiedFolderRepository(
    private val displayUnifiedFolderFlows: Map<FolderRole, Flow<UnifiedDisplayFolder>> = emptyMap(),
) : UnifiedFolderRepository {
    override fun getUnifiedDisplayFolderFlow(role: FolderRole): Flow<UnifiedDisplayFolder> {
        return displayUnifiedFolderFlows.getValue(role)
    }
}
