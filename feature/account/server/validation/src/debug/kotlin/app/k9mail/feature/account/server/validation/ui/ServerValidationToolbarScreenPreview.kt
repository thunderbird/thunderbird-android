package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeIncomingServerValidationViewModel

@Composable
@PreviewDevices
internal fun IncomingServerValidationToolbarScreenPreview() {
    PreviewWithTheme {
        ServerValidationToolbarScreen(
            title = "Incoming server settings",
            viewModel = FakeIncomingServerValidationViewModel(
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
        )
    }
}

@Composable
@PreviewDevices
internal fun OutgoingServerValidationToolbarScreenPreview() {
    PreviewWithTheme {
        ServerValidationToolbarScreen(
            title = "Incoming server settings",
            viewModel = FakeIncomingServerValidationViewModel(
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
        )
    }
}
