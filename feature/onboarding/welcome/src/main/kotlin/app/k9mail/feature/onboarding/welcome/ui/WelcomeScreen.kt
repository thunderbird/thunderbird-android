package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    WelcomeContent(
        onStartClick = onStartClick,
        onImportClick = onImportClick,
    )
}

@Composable
@PreviewDevices
internal fun WelcomeScreenThunderbirdPreview() {
    ThunderbirdTheme {
        WelcomeScreen(
            onStartClick = {},
            onImportClick = {},
        )
    }
}
