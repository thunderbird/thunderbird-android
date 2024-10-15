package app.k9mail.feature.account.setup.ui.options.display

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository

@Composable
@Preview(showBackground = true)
internal fun DisplayOptionsScreenPreview() {
    PreviewWithThemeAndKoin {
        DisplayOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = DisplayOptionsViewModel(
                validator = DisplayOptionsValidator(),
                accountStateRepository = FakeAccountStateRepository(),
                accountOwnerNameProvider = { null },
            ),
        )
    }
}
