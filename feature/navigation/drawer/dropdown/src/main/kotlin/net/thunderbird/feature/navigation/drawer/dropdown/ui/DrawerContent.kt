package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountView
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.getDisplayCutOutHorizontalInsetPadding
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.DRAWER_WIDTH
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getAdditionalWidth
import net.thunderbird.feature.navigation.drawer.dropdown.ui.folder.FolderList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.setting.AccountSettingList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.setting.FolderSettingList

@Composable
internal fun DrawerContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val additionalWidth = getAdditionalWidth()

    Surface(
        modifier = modifier
            .width(DRAWER_WIDTH + additionalWidth)
            .fillMaxHeight()
            .testTagAsResourceId("DrawerContent"),
        color = MainTheme.colors.surfaceContainerLow,
    ) {
        val selectedAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId }
        val horizontalInsetPadding = getDisplayCutOutHorizontalInsetPadding()

        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .windowInsetsPadding(horizontalInsetPadding),
        ) {
            selectedAccount?.let {
                AccountView(
                    account = selectedAccount,
                    onClick = { onEvent(Event.OnAccountSelectorClick) },
                    showAccountSelection = state.showAccountSelection,
                )

                DividerHorizontal()
            }
            AnimatedContent(
                targetState = state.showAccountSelection,
                label = "AccountSelectorVisibility",
                transitionSpec = {
                    if (targetState) {
                        slideInVertically { -it } togetherWith slideOutVertically { it }
                    } else {
                        slideInVertically { it } togetherWith slideOutVertically { -it }
                    }
                },
            ) { targetState ->
                if (targetState) {
                    AccountContent(
                        state = state,
                        onEvent = onEvent,
                        selectedAccount = selectedAccount,
                    )
                } else {
                    FolderContent(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountContent(
    state: State,
    onEvent: (Event) -> Unit,
    selectedAccount: DisplayAccount?,
) {
    Surface(
        color = MainTheme.colors.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            AccountList(
                accounts = state.accounts,
                selectedAccount = selectedAccount,
                onAccountClick = { onEvent(Event.OnAccountClick(it)) },
                showStarredCount = state.config.showStarredCount,
                modifier = Modifier.weight(1f),
            )
            DividerHorizontal()
            AccountSettingList(
                onAddAccountClick = { onEvent(Event.OnAddAccountClick) },
                onSyncAllAccountsClick = { onEvent(Event.OnSyncAllAccounts) },
            )
        }
    }
}

@Composable
private fun FolderContent(
    state: State,
    onEvent: (Event) -> Unit,
) {
    val isUnifiedAccount = remember(state.selectedAccountId) {
        state.accounts.any { it.id == state.selectedAccountId && it is UnifiedDisplayAccount }
    }

    Surface(
        color = MainTheme.colors.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            FolderList(
                rootFolder = state.rootFolder,
                selectedFolder = state.selectedFolder,
                onFolderClick = { folder ->
                    onEvent(Event.OnFolderClick(folder))
                },
                showStarredCount = state.config.showStarredCount,
                modifier = Modifier.weight(1f),
            )
            DividerHorizontal()
            FolderSettingList(
                onManageFoldersClick = { onEvent(Event.OnManageFoldersClick) },
                onSettingsClick = { onEvent(Event.OnSettingsClick) },
                isUnifiedAccount = isUnifiedAccount,
            )
        }
    }
}
