package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository
import app.k9mail.feature.account.server.certificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import com.fsck.k9.mail.server.ServerSettingsValidationResult

@Composable
@PreviewDevices
internal fun IncomingServerValidationScreenPreview() {
    PreviewWithThemeAndKoin {
        ServerValidationScreen(
            onNext = { },
            onBack = { },
            viewModel = IncomingServerValidationViewModel(
                accountStateRepository = FakeAccountStateRepository(),
                validateServerSettings = { ServerSettingsValidationResult.Success },
                authorizationStateRepository = { true },
                certificateErrorRepository = InMemoryServerCertificateErrorRepository(),
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
        )
    }
}

@Composable
@PreviewDevices
internal fun OutgoingServerValidationScreenPreview() {
    PreviewWithThemeAndKoin {
        ServerValidationScreen(
            onNext = { },
            onBack = { },
            viewModel = OutgoingServerValidationViewModel(
                accountStateRepository = FakeAccountStateRepository(),
                validateServerSettings = { ServerSettingsValidationResult.Success },
                authorizationStateRepository = { true },
                certificateErrorRepository = InMemoryServerCertificateErrorRepository(),
                oAuthViewModel = FakeAccountOAuthViewModel(),
            ),
        )
    }
}
