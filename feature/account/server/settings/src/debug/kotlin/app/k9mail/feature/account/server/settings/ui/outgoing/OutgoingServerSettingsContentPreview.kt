package app.k9mail.feature.account.server.settings.ui.outgoing

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import net.thunderbird.core.ui.common.annotation.PreviewDevices

@Composable
@PreviewDevices
internal fun OutgoingServerSettingsContentPreview() {
    PreviewWithTheme {
        OutgoingServerSettingsContent(
            mode = InteractionMode.Create,
            state = OutgoingServerSettingsContract.State(),
            onEvent = { },
        )
    }
}
