package app.k9mail.feature.account.server.settings.ui.outgoing

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.ui.fake.FakeAccountStateRepository
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewDevices
internal fun OutgoingServerSettingsScreenPreview() {
    ThundermailPreview {
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
            brandNameProvider = object : BrandNameProvider {
                override val brandName: String = "Thunderbird"
            },
        )
    }
}
