package app.k9mail.feature.account.setup.ui.options.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository

@Composable
@Preview(showBackground = true)
internal fun SyncOptionsScreenPreview() {
    PreviewWithThemeAndKoin {
        SyncOptionsScreen(
            onNext = {},
            onBack = {},
            viewModel = SyncOptionsViewModel(
                accountStateRepository = FakeAccountStateRepository(),
            ),
        )
    }
}
