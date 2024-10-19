package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewThemeType
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@PreviewDevices
internal fun WelcomeContentThunderbirdPreview() {
    PreviewWithTheme(themeType = PreviewThemeType.THUNDERBIRD) {
        WelcomeContent(
            onStartClick = {},
            onImportClick = {},
            appName = "Thunderbird Beta",
            showImportButton = false,
        )
    }
}

@Composable
@PreviewDevices
internal fun WelcomeContentK9MailPreview() {
    PreviewWithTheme(themeType = PreviewThemeType.K9MAIL) {
        WelcomeContent(
            onStartClick = {},
            onImportClick = {},
            appName = "K-9 Mail",
            showImportButton = true,
        )
    }
}
