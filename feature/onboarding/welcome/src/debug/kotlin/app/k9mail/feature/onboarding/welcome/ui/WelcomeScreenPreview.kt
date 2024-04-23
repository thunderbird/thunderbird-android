package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@PreviewDevices
internal fun WelcomeScreenThunderbirdPreview() {
    PreviewWithTheme {
        WelcomeScreen(
            onStartClick = {},
            onImportClick = {},
        )
    }
}
