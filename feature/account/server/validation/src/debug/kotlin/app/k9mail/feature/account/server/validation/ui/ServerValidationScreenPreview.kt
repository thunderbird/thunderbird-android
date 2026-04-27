package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository
import app.k9mail.feature.account.server.certificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeBrandNameProvider
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewDevices
internal fun IncomingServerValidationScreenPreview() {
    ThundermailPreview {
        ServerValidationScreen(
            onNext = { },
            onBack = { },
            viewModel = viewModel {
                IncomingServerValidationViewModel(
                    accountStateRepository = FakeAccountStateRepository(),
                    validateServerSettings = { ServerSettingsValidationResult.Success },
                    authorizationStateRepository = { true },
                    certificateErrorRepository = InMemoryServerCertificateErrorRepository(),
                    oAuthViewModel = FakeAccountOAuthViewModel(),
                )
            },
            animatedVisibilityScope = it,
            brandNameProvider = FakeBrandNameProvider,
        )
    }
}

@Composable
@PreviewDevices
internal fun OutgoingServerValidationScreenPreview() {
    ThundermailPreview {
        ServerValidationScreen(
            onNext = { },
            onBack = { },
            viewModel = viewModel {
                OutgoingServerValidationViewModel(
                    accountStateRepository = FakeAccountStateRepository(),
                    validateServerSettings = { ServerSettingsValidationResult.Success },
                    authorizationStateRepository = { true },
                    certificateErrorRepository = InMemoryServerCertificateErrorRepository(),
                    oAuthViewModel = FakeAccountOAuthViewModel(),
                )
            },
            animatedVisibilityScope = it,
            brandNameProvider = FakeBrandNameProvider,
        )
    }
}
