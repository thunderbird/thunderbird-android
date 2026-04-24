package app.k9mail.feature.account.setup.ui.options.display

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository
import app.k9mail.feature.account.setup.ui.fake.FakeBrandNameProvider
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@Preview(showBackground = true)
internal fun DisplayOptionsScreenPreview() {
    ThundermailPreview {
        DisplayOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = viewModel {
                DisplayOptionsViewModel(
                    validator = DisplayOptionsValidator(),
                    accountStateRepository = FakeAccountStateRepository(),
                    accountOwnerNameProvider = { null },
                )
            },
            brandNameProvider = FakeBrandNameProvider,
        )
    }
}
