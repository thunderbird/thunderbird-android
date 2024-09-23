package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.R
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.ui.setting.SettingItem
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AccountList(
    accounts: ImmutableList<DisplayAccount>,
    selectedAccount: DisplayAccount?,
    onAccountClick: (DisplayAccount) -> Unit,
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
                .width(MainTheme.sizes.large),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = MainTheme.spacings.default),
            ) {
                items(
                    items = accounts,
                    key = { account -> account.account.uuid },
                ) { account ->
                    if (selectedAccount != null && account == selectedAccount) {
                        return@items
                    }
                    AccountListItem(
                        account = account,
                        onClick = { onAccountClick(account) },
                    )
                }
            }
            SettingItem(
                label = stringResource(id = R.string.navigation_drawer_action_settings),
                onClick = onSettingsClick,
            )
        }
    }
}
