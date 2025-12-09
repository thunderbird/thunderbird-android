package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode

@Composable
@PreviewDevices
internal fun IncomingServerSettingsContentPreview() {
    PreviewWithTheme {
        IncomingServerSettingsContent(
            mode = InteractionMode.Create,
            onEvent = { },
            state = IncomingServerSettingsContract.State(),
        )
    }
}
