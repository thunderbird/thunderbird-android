package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.fake.FakeBrandNameProvider
import net.thunderbird.core.ui.common.annotation.PreviewDevicesWithBackground

@Composable
@PreviewDevicesWithBackground
internal fun AccountAutoDiscoveryScreenPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryScreen(
            onNext = {},
            onBack = {},
            onThundermailClick = {},
            onScanQrCodeClick = {},
            viewModel = viewModel {
                AccountAutoDiscoveryViewModel(
                    validator = AccountAutoDiscoveryValidator(),
                    getAutoDiscovery = { _, _ -> AutoDiscoveryResult.NoUsableSettingsFound },
                    accountStateRepository = FakeAccountStateRepository(),
                    oAuthViewModel = FakeAccountOAuthViewModel(),
                )
            },
            brandNameProvider = FakeBrandNameProvider,
        )
    }
}

@Composable
@PreviewDevicesWithBackground
internal fun AccountAutoDiscoveryScreenExpandedNetworkSettingsPreview() {
    PreviewWithTheme {
        AccountAutoDiscoveryScreen(
            onNext = {},
            onBack = {},
            onThundermailClick = {},
            onScanQrCodeClick = {},
            viewModel = viewModel {
                AccountAutoDiscoveryViewModel(
                    validator = AccountAutoDiscoveryValidator(),
                    getAutoDiscovery = { _, _ -> AutoDiscoveryResult.NoUsableSettingsFound },
                    accountStateRepository = FakeAccountStateRepository(),
                    oAuthViewModel = FakeAccountOAuthViewModel(),
                    initialState = AccountAutoDiscoveryContract.State(
                        isNetworkSettingsExpanded = true,
                    ),
                )
            },
            brandNameProvider = FakeBrandNameProvider,
        )
    }
}
