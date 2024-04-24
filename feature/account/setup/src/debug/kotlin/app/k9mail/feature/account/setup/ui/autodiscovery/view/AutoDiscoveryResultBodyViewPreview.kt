package app.k9mail.feature.account.setup.ui.autodiscovery.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.feature.account.setup.ui.autodiscovery.fake.fakeAutoDiscoveryResultSettings

@Composable
@Preview(showBackground = true)
internal fun AutoDiscoveryResultBodyViewPreview() {
    PreviewWithThemes {
        AutoDiscoveryResultBodyView(
            settings = fakeAutoDiscoveryResultSettings(isTrusted = true),
            onEditConfigurationClick = {},
        )
    }
}
