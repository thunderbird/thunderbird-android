package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.TreeFolder

internal class GetTreeFolders : UseCase.GetTreeFolders {
    override fun invoke(folders: List<DisplayFolder>, maxDepth: Int): TreeFolder {
        return TreeFolder.createFromFolders(folders, maxDepth)
    }
}
