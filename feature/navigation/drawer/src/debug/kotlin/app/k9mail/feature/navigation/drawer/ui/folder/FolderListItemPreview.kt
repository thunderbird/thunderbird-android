package app.k9mail.feature.navigation.drawer.ui.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.feature.navigation.drawer.ui.FakeData.DISPLAY_FOLDER
import app.k9mail.feature.navigation.drawer.ui.FakeData.UNIFIED_FOLDER

@Composable
@Preview(showBackground = true)
internal fun FolderListItemPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selected = false,
            showStarredCount = false,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemSelectedPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selected = true,
            showStarredCount = false,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemWithStarredPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selected = false,
            showStarredCount = true,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemWithStarredSelectedPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selected = true,
            showStarredCount = true,
            onClick = {},
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
            selected = false,
            showStarredCount = true,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemWithUnifiedFolderPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = UNIFIED_FOLDER,
            selected = false,
            showStarredCount = false,
            onClick = {},
        )
    }
}
