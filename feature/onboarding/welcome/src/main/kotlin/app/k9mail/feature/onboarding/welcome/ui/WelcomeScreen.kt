package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
@Preview
internal fun WelcomeScreenThunderbirdPreview() {
    ThunderbirdTheme {
        WelcomeScreen(
            onStartClick = {},
            onImportClick = {},
        )
    }
}
