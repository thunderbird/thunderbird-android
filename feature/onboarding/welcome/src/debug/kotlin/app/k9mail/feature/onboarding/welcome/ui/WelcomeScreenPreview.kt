package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@PreviewDevices
internal fun WelcomeScreenPreview() {
    PreviewWithTheme {
        WelcomeScreen(
            onStartClick = {},
            onImportClick = {},
            appNameProvider = object : AppNameProvider {
                override val appName: String = "AppName"
            },
        )
    }
}
