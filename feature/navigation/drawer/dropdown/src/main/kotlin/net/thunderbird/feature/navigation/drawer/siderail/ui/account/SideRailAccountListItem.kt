package net.thunderbird.feature.navigation.drawer.siderail.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountAvatar

@Composable
internal fun SideRailAccountListItem(
    account: MailDisplayAccount,
    onClick: (MailDisplayAccount) -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(MainTheme.sizes.large)
            .padding(vertical = MainTheme.spacings.half),
        contentAlignment = Alignment.Center,
    ) {
        AccountAvatar(
            account = account,
            onClick = onClick,
            selected = selected,
        )
    }
}
