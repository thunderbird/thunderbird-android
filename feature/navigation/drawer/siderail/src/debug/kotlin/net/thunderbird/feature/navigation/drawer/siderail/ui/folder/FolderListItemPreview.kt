package net.thunderbird.feature.navigation.drawer.siderail.ui.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import net.thunderbird.feature.navigation.drawer.siderail.ui.FakeData.DISPLAY_FOLDER
import net.thunderbird.feature.navigation.drawer.siderail.ui.FakeData.UNIFIED_FOLDER

@Composable
@Preview(showBackground = true)
internal fun FolderListItemPreview() {
    PreviewWithThemes {
        FolderListItem(
            displayFolder = DISPLAY_FOLDER,
            selected = false,
            showStarredCount = false,
            onClick = {},
            folderNameFormatter = FolderNameFormatter(LocalContext.current.resources),
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
            folderNameFormatter = FolderNameFormatter(LocalContext.current.resources),
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
            folderNameFormatter = FolderNameFormatter(LocalContext.current.resources),
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
            folderNameFormatter = FolderNameFormatter(LocalContext.current.resources),
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
            folderNameFormatter = FolderNameFormatter(LocalContext.current.resources),
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
            folderNameFormatter = FolderNameFormatter(LocalContext.current.resources),
        )
    }
}
