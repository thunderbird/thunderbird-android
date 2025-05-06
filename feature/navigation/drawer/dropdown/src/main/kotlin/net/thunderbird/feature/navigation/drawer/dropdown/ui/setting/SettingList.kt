package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.R

@Composable
internal fun SettingList(
    onAccountSelectorClick: () -> Unit,
    onManageFoldersClick: () -> Unit,
    showAccountSelector: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(vertical = MainTheme.spacings.default)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth(),
    ) {
        SettingListItem(
            label = stringResource(R.string.navigation_drawer_dropdown_action_manage_folders),
            onClick = onManageFoldersClick,
            imageVector = Icons.Outlined.FolderManaged,
        )
        SettingListItem(
            label = if (showAccountSelector) {
                stringResource(R.string.navigation_drawer_dropdown_action_hide_accounts)
            } else {
                stringResource(R.string.navigation_drawer_dropdown_action_show_accounts)
            },
            onClick = onAccountSelectorClick,
            imageVector = if (showAccountSelector) {
                Icons.Outlined.ChevronLeft
            } else {
                Icons.Outlined.ChevronRight
            },
        )
    }
}
