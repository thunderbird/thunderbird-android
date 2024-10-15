package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeIncomingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeOutgoingServerValidationViewModel

@Composable
@PreviewDevices
internal fun IncomingServerValidationMainScreenPreview() {
    PreviewWithThemeAndKoin {
        ServerValidationMainScreen(
            viewModel = FakeIncomingServerValidationViewModel(
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
        )
    }
}

@Composable
@PreviewDevices
internal fun OutgoingServerValidationMainScreenPreview() {
    PreviewWithThemeAndKoin {
        ServerValidationMainScreen(
            viewModel = FakeOutgoingServerValidationViewModel(
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
        )
    }
}
