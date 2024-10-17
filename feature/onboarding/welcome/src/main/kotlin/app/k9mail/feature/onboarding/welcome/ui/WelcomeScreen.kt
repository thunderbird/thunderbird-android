package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.feature.onboarding.migration.api.OnboardingMigrationManager

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    appNameProvider: AppNameProvider,
    onboardingMigrationManager: OnboardingMigrationManager,
) {
    WelcomeContent(
        onStartClick = onStartClick,
        onImportClick = onImportClick,
        appName = appNameProvider.appName,
        showImportButton = !onboardingMigrationManager.isFeatureIncluded(),
    )
}
