package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.server.validation.ui.fake.FakeAccountOAuthViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeBrandNameProvider
import app.k9mail.feature.account.server.validation.ui.fake.FakeIncomingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeOutgoingServerValidationViewModel
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewDevices
internal fun IncomingServerValidationMainScreenPreview() {
    ThundermailPreview {
        ServerValidationMainScreen(
            viewModel = viewModel {
                FakeIncomingServerValidationViewModel(
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
internal fun OutgoingServerValidationMainScreenPreview() {
    ThundermailPreview {
        ServerValidationMainScreen(
            viewModel = viewModel {
                FakeOutgoingServerValidationViewModel(
                    oAuthViewModel = FakeAccountOAuthViewModel(),
                )
            },
            animatedVisibilityScope = it,
            brandNameProvider = FakeBrandNameProvider,
        )
    }
}
