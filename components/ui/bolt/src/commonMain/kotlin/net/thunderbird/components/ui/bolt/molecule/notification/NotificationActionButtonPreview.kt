
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.molecule.notification.NotificationActionButton
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
