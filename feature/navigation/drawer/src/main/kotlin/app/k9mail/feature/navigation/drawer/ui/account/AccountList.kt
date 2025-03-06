package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.R
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.ui.setting.SettingItem
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AccountList(
    accounts: ImmutableList<DisplayAccount>,
    selectedAccount: DisplayAccount?,
    onAccountClick: (DisplayAccount) -> Unit,
    onSyncAllAccountsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MainTheme.colors.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                .width(MainTheme.sizes.large),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = MainTheme.spacings.default),
            ) {
                items(
                    items = accounts,
                    key = { account -> account.id },
                ) { account ->
                    AccountListItem(
                        account = account,
                        onClick = { onAccountClick(account) },
                        selected = selectedAccount == account,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(vertical = MainTheme.spacings.oneHalf),
            ) {
                SettingItem(
                    icon = Icons.Outlined.Sync,
                    label = stringResource(id = R.string.navigation_drawer_action_sync_all_accounts),
                    onClick = onSyncAllAccountsClick,
                )
                // Hack to compensate the column placement at an uneven coordinate, caused by the 1.dp divider.
                Spacer(modifier = Modifier.height(7.dp))
                SettingItem(
                    icon = Icons.Outlined.Settings,
                    label = stringResource(id = R.string.navigation_drawer_action_settings),
                    onClick = onSettingsClick,
                )
            }
        }
    }
}
