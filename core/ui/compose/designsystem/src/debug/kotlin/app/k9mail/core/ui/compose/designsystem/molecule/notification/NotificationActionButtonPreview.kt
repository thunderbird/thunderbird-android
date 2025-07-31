
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.molecule.notification.NotificationActionButton
import app.k9mail.core.ui.compose.theme2.MainTheme

@PreviewLightDark
@Composable
private fun NotificationActionButtonPreview() {
    PreviewWithThemesLightDark {
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            NotificationActionButton(
                onClick = {},
                text = "Sign in",
            )
            NotificationActionButton(
                onClick = {},
                text = "View support article",
                isExternalLink = true,
            )
        }
    }
}
