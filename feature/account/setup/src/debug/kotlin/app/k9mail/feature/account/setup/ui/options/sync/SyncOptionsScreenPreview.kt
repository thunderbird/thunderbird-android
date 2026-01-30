package app.k9mail.feature.account.setup.ui.options.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository
import app.k9mail.feature.account.setup.ui.fake.FakeBrandNameProvider

@Composable
@Preview(showBackground = true)
internal fun SyncOptionsScreenPreview() {
    PreviewWithTheme {
        SyncOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = viewModel {
                SyncOptionsViewModel(
                    accountStateRepository = FakeAccountStateRepository(),
                )
            },
            brandNameProvider = FakeBrandNameProvider,
        )
    }
}
