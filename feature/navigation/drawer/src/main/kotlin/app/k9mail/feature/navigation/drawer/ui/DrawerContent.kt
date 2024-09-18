package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.feature.navigation.drawer.ui.account.AccountList
import app.k9mail.feature.navigation.drawer.ui.account.AccountView
import app.k9mail.feature.navigation.drawer.ui.folder.FolderList
import app.k9mail.feature.navigation.drawer.ui.setting.SettingList

@Composable
fun DrawerContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("DrawerContent"),
    ) {
        Column {
            state.selectedAccount?.let {
                AccountView(
                    account = it,
                    onClick = { onEvent(Event.OnAccountViewClick(it)) },
                    showAvatar = state.showAccountSelector,
                )

                DividerHorizontal()
            }
            Row {
                AnimatedVisibility(
                    visible = state.showAccountSelector,
                ) {
                    AccountList(
                        accounts = state.accounts,
                        selectedAccount = state.selectedAccount,
                        onAccountClick = { onEvent(Event.OnAccountClick(it)) },
                        onSettingsClick = { onEvent(Event.OnSettingsClick) },
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    FolderList(
                        folders = state.folders,
                        selectedFolder = state.selectedFolder,
                        onFolderClick = { folder ->
                            onEvent(Event.OnFolderClick(folder))
                        },
                        showStarredCount = state.config.showStarredCount,
                        modifier = Modifier.weight(1f),
                    )
                    DividerHorizontal()
                    SettingList(
                        onAccountSelectorClick = { onEvent(Event.OnAccountSelectorClick) },
                        onManageFoldersClick = { onEvent(Event.OnManageFoldersClick) },
                        showAccountSelector = state.showAccountSelector,
                    )
                }
            }
        }
    }
}
