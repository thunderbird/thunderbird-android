package app.k9mail.feature.onboarding.migration.noop

import androidx.compose.runtime.Composable
import app.k9mail.feature.onboarding.migration.api.OnboardingMigrationManager

class NoOpOnboardingMigrationManager : OnboardingMigrationManager {
    override fun isFeatureIncluded(): Boolean = false

    @Composable
    override fun OnboardingMigrationScreen(
        onQrCodeScan: () -> Unit,
        onAddAccount: () -> Unit,
        onImport: () -> Unit,
    ) = Unit
}
