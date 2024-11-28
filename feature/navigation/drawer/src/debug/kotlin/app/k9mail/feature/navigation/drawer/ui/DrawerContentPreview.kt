package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.navigation.drawer.ui.FakeData.DISPLAY_ACCOUNT
import app.k9mail.feature.navigation.drawer.ui.FakeData.DISPLAY_FOLDER
import app.k9mail.feature.navigation.drawer.ui.FakeData.UNIFIED_FOLDER
import kotlinx.collections.immutable.persistentListOf

@Composable
@Preview(showBackground = true)
internal fun DrawerContentPreview() {
    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = persistentListOf(),
                selectedAccountId = null,
                folders = persistentListOf(),
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentWithAccountPreview() {
    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = persistentListOf(DISPLAY_ACCOUNT),
                selectedAccountId = DISPLAY_ACCOUNT.id,
                folders = persistentListOf(),
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentWithFoldersPreview() {
    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = persistentListOf(
                    DISPLAY_ACCOUNT,
                ),
                selectedAccountId = null,
                folders = persistentListOf(
                    UNIFIED_FOLDER,
                    DISPLAY_FOLDER,
                ),
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentWithSelectedFolderPreview() {
    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = persistentListOf(
                    DISPLAY_ACCOUNT,
                ),
                selectedAccountId = DISPLAY_ACCOUNT.id,
                folders = persistentListOf(
                    UNIFIED_FOLDER,
                    DISPLAY_FOLDER,
                ),
                selectedFolderId = DISPLAY_FOLDER.id,
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentWithSelectedUnifiedFolderPreview() {
    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = persistentListOf(
                    DISPLAY_ACCOUNT,
                ),
                selectedAccountId = DISPLAY_ACCOUNT.id,
                folders = persistentListOf(
                    UNIFIED_FOLDER,
                    DISPLAY_FOLDER,
                ),
                selectedFolderId = UNIFIED_FOLDER.id,
            ),
            onEvent = {},
        )
    }
}
