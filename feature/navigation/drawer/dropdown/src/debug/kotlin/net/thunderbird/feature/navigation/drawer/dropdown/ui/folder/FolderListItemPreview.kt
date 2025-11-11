package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.DISPLAY_FOLDER
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.DISPLAY_TREE_FOLDER_WITH_UNIFIED_FOLDER
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.UNIFIED_FOLDER

@Composable
@Preview(showBackground = true)
internal fun FolderListItemPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selectedFolderId = "unknown",
            showStarredCount = false,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalResources.current),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemSelectedPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selectedFolderId = DISPLAY_FOLDER.id,
            showStarredCount = false,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalResources.current),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemWithStarredPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selectedFolderId = "unknown",
            showStarredCount = true,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalResources.current),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemWithStarredSelectedPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selectedFolderId = DISPLAY_FOLDER.id,
            showStarredCount = true,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalResources.current),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemWithInboxFolderPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER.copy(
                folder = DISPLAY_FOLDER.folder.copy(
                    type = FolderType.INBOX,
                ),
            ),
            selectedFolderId = "unknown",
            showStarredCount = true,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalResources.current),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemWithUnifiedFolderPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = UNIFIED_FOLDER,
            selectedFolderId = "unknown",
            showStarredCount = false,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalResources.current),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemWithUnifiedFolderSelectedPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = UNIFIED_FOLDER,
            treeFolder = DISPLAY_TREE_FOLDER_WITH_UNIFIED_FOLDER,
            selectedFolderId = UNIFIED_FOLDER.id,
            showStarredCount = false,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalResources.current),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemStarredCountPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = UNIFIED_FOLDER,
            treeFolder = DISPLAY_TREE_FOLDER_WITH_UNIFIED_FOLDER,
            selectedFolderId = null,
            showStarredCount = true,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalResources.current),
        )
    }
}
