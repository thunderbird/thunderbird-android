package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeIncomingServerValidationViewModel
import net.thunderbird.core.ui.common.annotation.PreviewDevices

@Composable
@PreviewDevices
internal fun IncomingServerValidationToolbarScreenPreview() {
    PreviewWithTheme {
        ServerValidationToolbarScreen(
            title = "Incoming server settings",
            viewModel = viewModel {
                FakeIncomingServerValidationViewModel(
                    oAuthViewModel = FakeAccountOAuthViewModel(),
                )
            },
        )
    }
}

@Composable
@PreviewDevices
internal fun OutgoingServerValidationToolbarScreenPreview() {
    PreviewWithTheme {
        ServerValidationToolbarScreen(
            title = "Incoming server settings",
            viewModel = viewModel {
                FakeIncomingServerValidationViewModel(
                    oAuthViewModel = FakeAccountOAuthViewModel(),
                )
            },
        )
    }
}
