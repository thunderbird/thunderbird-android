package app.k9mail.feature.account.server.settings.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ServerSettingsPasswordInputCreatePreview() {
    PreviewWithThemes {
        ServerSettingsPasswordInput(
            mode = InteractionMode.Create,
            onPasswordChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ServerSettingsPasswordInputEditPreview() {
    PreviewWithThemes {
        ServerSettingsPasswordInput(
            mode = InteractionMode.Edit,
            onPasswordChange = {},
        )
    }
}
