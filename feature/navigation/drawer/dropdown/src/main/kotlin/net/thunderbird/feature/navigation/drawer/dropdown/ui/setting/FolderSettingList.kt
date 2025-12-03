package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.window.WindowSizeClass
import app.k9mail.core.ui.compose.common.window.getWindowSizeInfo
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.navigation.drawer.dropdown.R

@Composable
internal fun FolderSettingList(
    onSyncAccountClick: () -> Unit,
    onManageFoldersClick: () -> Unit,
    onSyncAllAccountsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isUnifiedAccount: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val windowSizeInfo = getWindowSizeInfo()
    val isLandscape = windowSizeInfo.screenWidth > windowSizeInfo.screenHeight
    val isCompactHeight = windowSizeInfo.screenHeightSizeClass == WindowSizeClass.Compact
    val hideText = isLandscape && isCompactHeight

    SettingList(
        modifier = modifier
            .padding(vertical = MainTheme.spacings.default)
            .fillMaxWidth(),
    ) {
        if (isUnifiedAccount.not()) {
            item(span = { if (hideText) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) {
                SettingListItem(
                    label = stringResource(id = R.string.navigation_drawer_dropdown_action_sync_account),
                    onClick = onSyncAccountClick,
                    icon = Icons.Outlined.Sync,
                    isLoading = isLoading,
                )
            }
            item(span = { if (hideText) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) {
                SettingListItem(
                    label = stringResource(R.string.navigation_drawer_dropdown_action_manage_folders),
                    onClick = onManageFoldersClick,
                    icon = Icons.Outlined.FolderManaged,
                )
            }
        } else {
            item(span = { if (hideText) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) {
                SettingListItem(
                    label = stringResource(id = R.string.navigation_drawer_dropdown_action_sync_all_accounts),
                    onClick = onSyncAllAccountsClick,
                    icon = Icons.Outlined.Sync,
                    isLoading = isLoading,
                )
            }
        }
        item(span = { if (hideText) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) {
            SettingListItem(
                label = stringResource(id = R.string.navigation_drawer_dropdown_action_settings),
                onClick = onSettingsClick,
                icon = Icons.Outlined.Settings,
            )
        }
    }
}
