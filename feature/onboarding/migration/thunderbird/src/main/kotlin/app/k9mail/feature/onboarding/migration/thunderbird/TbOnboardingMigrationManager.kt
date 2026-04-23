package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.runtime.Composable
import app.k9mail.feature.onboarding.migration.api.OnboardingMigrationManager
import net.thunderbird.feature.settings.import.ui.ImportAccountScreen

class TbOnboardingMigrationManager : OnboardingMigrationManager {
    override fun isFeatureIncluded(): Boolean = true

    @Composable
    override fun OnboardingMigrationScreen(
        onQrCodeScan: () -> Unit,
        onAddAccount: () -> Unit,
        onImport: () -> Unit,
    ) {
        ImportAccountScreen(
            onQrCodeScan,
            onAddAccount,
            onImport,
            {},
        )
    }
}
