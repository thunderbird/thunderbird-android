package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount

@Composable
internal fun AccountListItem(
    account: DisplayAccount,
    onClick: (DisplayAccount) -> Unit,
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
        )
    }
}
