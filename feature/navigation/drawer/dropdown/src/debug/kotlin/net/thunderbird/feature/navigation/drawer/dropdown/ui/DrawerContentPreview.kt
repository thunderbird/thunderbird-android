package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.DISPLAY_FOLDER
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.MAIL_DISPLAY_ACCOUNT
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.UNIFIED_FOLDER
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.createAccountList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.createDisplayFolderList

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
                accounts = persistentListOf(MAIL_DISPLAY_ACCOUNT),
                selectedAccountId = MAIL_DISPLAY_ACCOUNT.id,
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
                    MAIL_DISPLAY_ACCOUNT,
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
                    MAIL_DISPLAY_ACCOUNT,
                ),
                selectedAccountId = MAIL_DISPLAY_ACCOUNT.id,
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
                    MAIL_DISPLAY_ACCOUNT,
                ),
                selectedAccountId = MAIL_DISPLAY_ACCOUNT.id,
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

@Composable
@Preview(showBackground = true)
internal fun DrawerContentSingleAccountPreview() {
    val displayFolders = createDisplayFolderList(hasUnifiedFolder = false)

    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = persistentListOf(
                    MAIL_DISPLAY_ACCOUNT,
                ),
                selectedAccountId = MAIL_DISPLAY_ACCOUNT.id,
                folders = displayFolders,
                selectedFolderId = displayFolders[0].id,
                config = DrawerConfig(
                    showUnifiedFolders = false,
                    showStarredCount = false,
                    showAccountSelector = false,
                    expandAllFolder = false,
                ),
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentSingleAccountWithAccountSelectionPreview() {
    val displayFolders = createDisplayFolderList(hasUnifiedFolder = false)

    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = persistentListOf(
                    MAIL_DISPLAY_ACCOUNT,
                ),
                selectedAccountId = MAIL_DISPLAY_ACCOUNT.id,
                folders = displayFolders,
                selectedFolderId = displayFolders[0].id,
                config = DrawerConfig(
                    showUnifiedFolders = false,
                    showStarredCount = false,
                    showAccountSelector = true,
                    expandAllFolder = false,
                ),
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentMultipleAccountsAccountPreview() {
    val accountList = createAccountList()
    val displayFolders = createDisplayFolderList(hasUnifiedFolder = true)

    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = accountList,
                selectedAccountId = accountList[0].id,
                folders = displayFolders,
                selectedFolderId = UNIFIED_FOLDER.id,
                config = DrawerConfig(
                    showUnifiedFolders = false,
                    showStarredCount = false,
                    showAccountSelector = false,
                    expandAllFolder = false,
                ),
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentMultipleAccountsWithAccountSelectionPreview() {
    val accountList = createAccountList()

    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = accountList,
                selectedAccountId = accountList[1].id,
                folders = createDisplayFolderList(hasUnifiedFolder = true),
                selectedFolderId = UNIFIED_FOLDER.id,
                config = DrawerConfig(
                    showUnifiedFolders = false,
                    showStarredCount = false,
                    showAccountSelector = true,
                    expandAllFolder = false,
                ),
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentMultipleAccountsWithDifferentAccountSelectionPreview() {
    val accountList = createAccountList()

    PreviewWithTheme {
        DrawerContent(
            state = DrawerContract.State(
                accounts = accountList,
                selectedAccountId = accountList[2].id,
                folders = createDisplayFolderList(hasUnifiedFolder = true),
                selectedFolderId = UNIFIED_FOLDER.id,
                config = DrawerConfig(
                    showUnifiedFolders = false,
                    showStarredCount = false,
                    showAccountSelector = true,
                    expandAllFolder = false,
                ),
            ),
            onEvent = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentSmallScreenPreview() {
    val accountList = createAccountList()

    PreviewWithTheme {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .height(480.dp),
        ) {
            DrawerContent(
                state = DrawerContract.State(
                    accounts = accountList,
                    selectedAccountId = accountList[2].id,
                    folders = createDisplayFolderList(hasUnifiedFolder = true),
                    selectedFolderId = UNIFIED_FOLDER.id,
                    config = DrawerConfig(
                        showUnifiedFolders = false,
                        showStarredCount = false,
                        showAccountSelector = true,
                        expandAllFolder = false,
                    ),
                ),
                onEvent = {},
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun DrawerContentVerySmallScreenPreview() {
    val accountList = createAccountList()

    PreviewWithTheme {
        Surface(
            modifier = Modifier
                .width(240.dp)
                .height(320.dp),
        ) {
            DrawerContent(
                state = DrawerContract.State(
                    accounts = accountList,
                    selectedAccountId = accountList[2].id,
                    folders = createDisplayFolderList(hasUnifiedFolder = true),
                    selectedFolderId = UNIFIED_FOLDER.id,
                    config = DrawerConfig(
                        showUnifiedFolders = false,
                        showStarredCount = false,
                        showAccountSelector = true,
                        expandAllFolder = false,
                    ),
                ),
                onEvent = {},
            )
        }
    }
}
