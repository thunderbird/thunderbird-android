package app.k9mail.feature.account.setup.ui.createaccount

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.ui.fake.FakeBrandNameProvider
import net.thunderbird.core.ui.common.annotation.PreviewDevices

@Composable
@PreviewDevices
internal fun AccountOptionsScreenK9Preview() {
    PreviewWithTheme {
        CreateAccountScreen(
            onNext = {},
            onBack = {},
            viewModel = viewModel {
                CreateAccountViewModel(
                    createAccount = { AccountCreatorResult.Success("irrelevant") },
                    accountStateRepository = InMemoryAccountStateRepository(),
                )
            },
            brandNameProvider = FakeBrandNameProvider,
        )
    }
}
