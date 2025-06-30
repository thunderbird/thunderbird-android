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
internal fun AccountSettingList(
    onSyncAllAccountsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingList(
        modifier = modifier
            .padding(vertical = MainTheme.spacings.default)
            .fillMaxWidth(),
    ) {
        item {
            SettingListItem(
                label = stringResource(id = R.string.navigation_drawer_dropdown_action_sync_all_accounts),
                onClick = onSyncAllAccountsClick,
                icon = Icons.Outlined.Sync,
            )
        }
    }
}
