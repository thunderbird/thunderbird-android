package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.runtime.Composable
import app.k9mail.feature.onboarding.migration.api.OnboardingMigrationManager

class TbOnboardingMigrationManager : OnboardingMigrationManager {
    override fun isFeatureIncluded(): Boolean = true

    @Composable
    override fun OnboardingMigrationScreen(
        onQrCodeScan: () -> Unit,
        onAddAccount: () -> Unit,
        onImport: () -> Unit,
    ) {
        TbOnboardingMigrationScreen(
            onQrCodeScan,
            onAddAccount,
            onImport,
        )
    }
}
