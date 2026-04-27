package app.k9mail.feature.account.oauth.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.common.annotation.PreviewDevices

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
