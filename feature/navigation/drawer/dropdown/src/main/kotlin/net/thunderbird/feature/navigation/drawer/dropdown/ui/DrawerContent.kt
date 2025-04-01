package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountView
import net.thunderbird.feature.navigation.drawer.dropdown.ui.folder.FolderList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.setting.SettingList

// As long as we use DrawerLayout, we don't have to worry about screens narrower than DRAWER_WIDTH. DrawerLayout will
// automatically limit the width of the content view so there's still room for a scrim with minimum tap width.
private val DRAWER_WIDTH = 360.dp

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
            .testTag("DrawerContent"),
    ) {
        val selectedAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId }
        Column {
            selectedAccount?.let {
                AccountView(
                    account = selectedAccount,
                    onClick = { onEvent(Event.OnAccountViewClick(selectedAccount)) },
                    showAvatar = state.config.showAccountSelector,
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
                        onSettingsClick = { onEvent(Event.OnSettingsClick) },
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
                        onAccountSelectorClick = { onEvent(Event.OnAccountSelectorClick) },
                        onManageFoldersClick = { onEvent(Event.OnManageFoldersClick) },
                        showAccountSelector = state.config.showAccountSelector,
                    )
                }
            }
        }
    }
}

@Composable
fun getAdditionalWidth(): Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    return if (isRtl) {
        WindowInsets.displayCutout.getRight(density = density, layoutDirection = layoutDirection)
    } else {
        WindowInsets.displayCutout.getLeft(density = density, layoutDirection = layoutDirection)
    }.pxToDp()
}

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }
