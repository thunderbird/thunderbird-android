package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.compose.runtime.Composable
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices

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
