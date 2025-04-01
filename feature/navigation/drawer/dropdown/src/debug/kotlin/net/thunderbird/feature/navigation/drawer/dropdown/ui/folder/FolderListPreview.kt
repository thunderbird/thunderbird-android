package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.TreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.DISPLAY_FOLDER
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.UNIFIED_FOLDER

@Composable
@Preview(showBackground = true)
internal fun FolderListPreview() {
    PreviewWithTheme {
        FolderList(
            rootFolder = TreeFolder.createFromFolders(
                persistentListOf(DISPLAY_FOLDER),
            ),
            selectedFolder = null,
            onFolderClick = {},
            showStarredCount = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListPreviewSelected() {
    PreviewWithTheme {
        FolderList(
            rootFolder = TreeFolder.createFromFolders(
                persistentListOf(DISPLAY_FOLDER),
            ),
            selectedFolder = DISPLAY_FOLDER,
            onFolderClick = {},
            showStarredCount = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListWithUnifiedFolderPreview() {
    PreviewWithTheme {
        FolderList(
            rootFolder = TreeFolder.createFromFolders(persistentListOf(
                UNIFIED_FOLDER,
                DISPLAY_FOLDER,
            )),
            selectedFolder = DISPLAY_FOLDER,
            onFolderClick = {},
            showStarredCount = false,
        )
    }
}
