package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.compose.runtime.Composable
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.fake.FakeAppNameProvider

@Composable
@PreviewDevicesWithBackground
internal fun AccountAutoDiscoveryScreenPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountAutoDiscoveryViewModel(
                validator = AccountAutoDiscoveryValidator(),
                getAutoDiscovery = { AutoDiscoveryResult.NoUsableSettingsFound },
                accountStateRepository = FakeAccountStateRepository(),
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
            appNameProvider = FakeAppNameProvider,
        )
    }
}
