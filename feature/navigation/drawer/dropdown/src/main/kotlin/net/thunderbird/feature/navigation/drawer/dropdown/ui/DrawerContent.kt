package net.thunderbird.feature.navigation.drawer.dropdown.ui

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountView
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.DRAWER_WIDTH
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getAdditionalWidth
import net.thunderbird.feature.navigation.drawer.dropdown.ui.folder.FolderList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.setting.AccountSettingList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.setting.FolderSettingList

private const val ANIMATION_DURATION_MS = 300

@Composable
private fun areSystemAnimationsEnabled(): Boolean {
    return Settings.Global.getFloat(
        LocalContext.current.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) != 0f
}

private fun accountSelectorTransitionSpec(
    areAnimationsEnabled: Boolean,
): AnimatedContentTransitionScope<Boolean>.() -> ContentTransform = {
    if (areAnimationsEnabled) {
        val animationSpec = tween<IntOffset>(durationMillis = ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
        if (targetState) {
            slideInVertically(animationSpec = animationSpec) { -it } togetherWith
                slideOutVertically(animationSpec = animationSpec) { it }
        } else {
            slideInVertically(animationSpec = animationSpec) { it } togetherWith
                slideOutVertically(animationSpec = animationSpec) { -it }
        }
    } else {
        slideInVertically(animationSpec = snap()) { 0 } togetherWith
            slideOutVertically(animationSpec = snap()) { 0 }
    }
}

@Composable
internal fun DrawerContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val additionalWidth = getAdditionalWidth()
    val areAnimationsEnabled = areSystemAnimationsEnabled()

    Surface(
        modifier = modifier
            .width(DRAWER_WIDTH + additionalWidth)
            .fillMaxHeight()
            .testTagAsResourceId("DrawerContent"),
        color = MainTheme.colors.surfaceContainerLow,
    ) {
        val selectedAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId }

        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            selectedAccount?.let {
                AccountView(
                    account = selectedAccount,
                    onClick = { onEvent(Event.OnAccountSelectorClick) },
                    onAvatarClick = { onEvent(Event.OnAccountViewClick(selectedAccount)) },
                    showAccountSelection = state.showAccountSelection,
                    isShowAnimations = areAnimationsEnabled,
                )

                DividerHorizontal()
            }
            AnimatedContent(
                targetState = state.showAccountSelection,
                label = "AccountSelectorVisibility",
                transitionSpec = accountSelectorTransitionSpec(areAnimationsEnabled),
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
                onSettingsClick = { onEvent(Event.OnSettingsClick) },
                isLoading = state.isLoading,
            )
        }
    }
}

@Composable
private fun FolderContent(
    state: State,
    onEvent: (Event) -> Unit,
) {
    val isUnifiedAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId } is UnifiedDisplayAccount

    Surface(
        color = MainTheme.colors.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            FolderList(
                isExpandedInitial = state.config.expandAllFolder,
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
                onSyncAccountClick = { onEvent(Event.OnSyncAccount) },
                onManageFoldersClick = { onEvent(Event.OnManageFoldersClick) },
                onSyncAllAccountsClick = { onEvent(Event.OnSyncAllAccounts) },
                onSettingsClick = { onEvent(Event.OnSettingsClick) },
                isUnifiedAccount = isUnifiedAccount,
                isLoading = state.isLoading,
            )
        }
    }
}
