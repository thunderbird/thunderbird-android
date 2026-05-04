package app.k9mail.feature.account.server.settings.ui.outgoing

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository
import net.thunderbird.core.ui.common.annotation.PreviewDevices

@Composable
@PreviewDevices
internal fun OutgoingServerSettingsScreenPreview() {
    PreviewWithTheme {
        OutgoingServerSettingsScreen(
            onNext = {},
            onBack = {},
            viewModel = viewModel {
                OutgoingServerSettingsViewModel(
                    mode = InteractionMode.Create,
                    validator = OutgoingServerSettingsValidator(),
                    accountStateRepository = FakeAccountStateRepository(),
                )
            },
        )
    }
}
