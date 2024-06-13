package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeAppNameProvider
import app.k9mail.feature.account.server.validation.ui.fake.FakeIncomingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeOutgoingServerValidationViewModel

@Composable
@PreviewDevices
internal fun IncomingServerValidationMainScreenPreview() {
    PreviewWithTheme {
        ServerValidationMainScreen(
            viewModel = FakeIncomingServerValidationViewModel(
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
            appNameProvider = FakeAppNameProvider,
        )
    }
}

@Composable
@PreviewDevices
internal fun OutgoingServerValidationMainScreenPreview() {
    PreviewWithTheme {
        ServerValidationMainScreen(
            viewModel = FakeOutgoingServerValidationViewModel(
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
            appNameProvider = FakeAppNameProvider,
        )
    }
}
