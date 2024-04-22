package app.k9mail.feature.account.server.settings.ui.outgoing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode

@Composable
@PreviewDevices
internal fun OutgoingServerSettingsContentPreview() {
    PreviewWithTheme {
        OutgoingServerSettingsContent(
            mode = InteractionMode.Create,
            state = OutgoingServerSettingsContract.State(),
            onEvent = { },
            contentPadding = PaddingValues(),
        )
    }
}
