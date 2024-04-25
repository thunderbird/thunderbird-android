package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.setup.ui.specialfolders.fake.FakeSpecialFoldersViewModel

@Composable
@PreviewDevices
internal fun SpecialFoldersScreenPreview() {
    PreviewWithTheme {
        SpecialFoldersScreen(
            onNext = {},
            onBack = {},
            viewModel = FakeSpecialFoldersViewModel(),
        )
    }
}
