package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.onboarding.migration.api.OnboardingMigrationManager
import net.thunderbird.core.common.provider.AppNameProvider

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    appNameProvider: AppNameProvider,
    onboardingMigrationManager: OnboardingMigrationManager,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        WelcomeContent(
            onStartClick = onStartClick,
            onImportClick = onImportClick,
            appName = appNameProvider.appName,
            showImportButton = !onboardingMigrationManager.isFeatureIncluded(),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
