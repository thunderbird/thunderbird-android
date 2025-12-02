package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.navigation.drawer.dropdown.R

@Composable
internal fun FolderSettingList(
    onSyncAccountClick: () -> Unit,
    onManageFoldersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isUnifiedAccount: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    SettingList(
        modifier = modifier
            .padding(vertical = MainTheme.spacings.default)
            .fillMaxWidth(),
    ) {
        if (isUnifiedAccount.not()) {
            item {
                SettingListItem(
                    label = stringResource(id = R.string.navigation_drawer_dropdown_action_sync_account),
                    onClick = onSyncAccountClick,
                    icon = Icons.Outlined.Sync,
                    isLoading = isLoading,
                )
            }
            item {
                SettingListItem(
                    label = stringResource(R.string.navigation_drawer_dropdown_action_manage_folders),
                    onClick = onManageFoldersClick,
                    icon = Icons.Outlined.FolderManaged,
                )
            }
        }
        item {
            SettingListItem(
                label = stringResource(id = R.string.navigation_drawer_dropdown_action_settings),
                onClick = onSettingsClick,
                icon = Icons.Outlined.Settings,
            )
        }
    }
}
