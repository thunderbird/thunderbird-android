package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountView
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.DRAWER_WIDTH
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getAdditionalWidth
import net.thunderbird.feature.navigation.drawer.dropdown.ui.folder.FolderList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.setting.SettingList

@Composable
internal fun DrawerContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val additionalWidth = getAdditionalWidth()

    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .width(DRAWER_WIDTH + additionalWidth)
            .fillMaxHeight()
            .testTagAsResourceId("DrawerContent"),
        color = MainTheme.colors.surfaceContainerLow,
    ) {
        val selectedAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId }
        Column {
            selectedAccount?.let {
                AccountView(
                    account = selectedAccount,
                    onClick = { onEvent(Event.OnAccountSelectorClick) },
                    showAccount = state.config.showAccountSelector,
                )

                DividerHorizontal()
            }
            Row {
                AnimatedVisibility(
                    visible = state.config.showAccountSelector,
                ) {
                    AccountList(
                        accounts = state.accounts,
                        selectedAccount = selectedAccount,
                        onAccountClick = { onEvent(Event.OnAccountClick(it)) },
                        onSyncAllAccountsClick = { onEvent(Event.OnSyncAllAccounts) },
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    FolderList(
                        rootFolder = state.rootFolder,
                        selectedFolder = state.folders.firstOrNull { it.id == state.selectedFolderId },
                        onFolderClick = { folder ->
                            onEvent(Event.OnFolderClick(folder))
                        },
                        showStarredCount = state.config.showStarredCount,
                        modifier = Modifier.weight(1f),
                    )
                    DividerHorizontal()
                    SettingList(
                        onManageFoldersClick = { onEvent(Event.OnManageFoldersClick) },
                        onSettingsClick = { onEvent(Event.OnSettingsClick) },
                    )
                }
            }
        }
    }
}
