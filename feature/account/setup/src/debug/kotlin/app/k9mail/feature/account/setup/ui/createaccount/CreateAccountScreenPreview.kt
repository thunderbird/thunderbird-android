package app.k9mail.feature.account.setup.ui.createaccount

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult

@Composable
@PreviewDevices
internal fun AccountOptionsScreenK9Preview() {
    PreviewWithThemeAndKoin {
        CreateAccountScreen(
            onNext = {},
            onBack = {},
            viewModel = CreateAccountViewModel(
                createAccount = { AccountCreatorResult.Success("irrelevant") },
                accountStateRepository = InMemoryAccountStateRepository(),
            ),
        )
    }
}
