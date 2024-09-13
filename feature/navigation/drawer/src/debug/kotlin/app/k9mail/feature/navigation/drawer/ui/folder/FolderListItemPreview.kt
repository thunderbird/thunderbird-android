package app.k9mail.feature.navigation.drawer.ui.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.feature.navigation.drawer.ui.FakeData.DISPLAY_FOLDER

@Composable
@Preview(showBackground = true)
fun FolderListItemPreview() {
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
fun FolderListItemSelectedPreview() {
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
fun FolderListItemWithStarredPreview() {
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
fun FolderListItemWithStarredSelectedPreview() {
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
fun FolderListItemWithInboxFolderPreview() {
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
