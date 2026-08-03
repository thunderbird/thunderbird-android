package app.k9mail.feature.account.oauth.ui

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices

@Composable
@PreviewDevices
internal fun AccountOAuthContentPreview() {
    PreviewWithTheme {
        AccountOAuthContent(
            state = AccountOAuthContract.State(),
            onEvent = {},
        )
    }
}
