package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewThemeType
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@PreviewDevices
internal fun WelcomeContentThunderbirdPreview() {
    ThundermailPreview {
        PreviewWithTheme(themeType = PreviewThemeType.THUNDERBIRD) {
            WelcomeContent(
                onStartClick = {},
                appName = "Thunderbird Beta",
                animatedVisibilityScope = it,
            )
        }
    }
}

@Composable
@PreviewDevices
internal fun WelcomeContentK9MailPreview() {
    ThundermailPreview {
        PreviewWithTheme(themeType = PreviewThemeType.K9MAIL) {
            WelcomeContent(
                onStartClick = {},
                appName = "K-9 Mail",
                animatedVisibilityScope = it,
            )
        }
    }
}
