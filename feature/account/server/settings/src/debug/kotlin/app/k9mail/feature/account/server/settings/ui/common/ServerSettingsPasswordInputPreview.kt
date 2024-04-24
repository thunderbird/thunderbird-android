package app.k9mail.feature.account.server.settings.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.feature.account.common.domain.entity.InteractionMode

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
