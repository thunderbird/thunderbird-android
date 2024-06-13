package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.common.provider.AppNameProvider

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    appNameProvider: AppNameProvider,
) {
    WelcomeContent(
        onStartClick = onStartClick,
        onImportClick = onImportClick,
        appName = appNameProvider.appName,
    )
}
