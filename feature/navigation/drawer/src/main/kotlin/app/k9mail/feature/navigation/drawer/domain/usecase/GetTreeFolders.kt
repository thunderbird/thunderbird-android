package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.TreeFolder

internal class GetTreeFolders : UseCase.GetTreeFolders {
    override fun invoke(folders: List<DisplayFolder>, maxDepth: Int): TreeFolder {
        return TreeFolder.createFromFolders(folders, maxDepth)
    }
}
