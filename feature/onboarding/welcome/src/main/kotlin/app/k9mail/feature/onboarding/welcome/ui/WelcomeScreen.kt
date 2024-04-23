package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable

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
