package app.k9mail.feature.account.edit.ui.server.settings.save

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.edit.ui.server.settings.save.fake.FakeSaveServerSettingsViewModel

@Composable
@Preview(showBackground = true)
internal fun SaveServerSettingsScreenK9Preview() {
    PreviewWithTheme {
        SaveServerSettingsScreen(
            title = "Incoming server settings",
            onNext = {},
            onBack = {},
            viewModel = viewModel {
                FakeSaveServerSettingsViewModel(
                    isIncoming = true,
                )
            },
        )
    }
}
