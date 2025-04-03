package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.R

@Composable
internal fun FolderSettingList(
    onManageFoldersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(vertical = MainTheme.spacings.default)
            .fillMaxWidth(),
    ) {
        SettingListItem(
            label = stringResource(R.string.navigation_drawer_dropdown_action_manage_folders),
            onClick = onManageFoldersClick,
            icon = Icons.Outlined.FolderManaged,
        )
        SettingListItem(
            label = stringResource(id = R.string.navigation_drawer_dropdown_action_settings),
            onClick = onSettingsClick,
            icon = Icons.Outlined.Settings,
        )
    }
}
