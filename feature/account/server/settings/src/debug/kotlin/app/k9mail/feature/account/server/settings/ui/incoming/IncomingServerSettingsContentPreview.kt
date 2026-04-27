package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewDevices
internal fun IncomingServerSettingsContentPreview() {
    ThundermailPreview {
        IncomingServerSettingsContent(
            mode = InteractionMode.Create,
            onEvent = { },
            contentPadding = PaddingValues(),
            maxWidth = Dp.Unspecified,
            state = IncomingServerSettingsContract.State(),
        )
    }
}
