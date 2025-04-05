package app.k9mail.feature

import app.k9mail.feature.funding.api.FundingSettings
import app.k9mail.feature.funding.featureFundingModule
import app.k9mail.feature.migration.launcher.featureMigrationModule
import app.k9mail.feature.onboarding.migration.onboardingMigrationModule
import app.k9mail.feature.telemetry.telemetryModule
import net.thunderbird.app.common.feature.featureLauncherModule
import net.thunderbird.feature.account.settings.featureAccountSettingsModule
import org.koin.dsl.module

val featureModule = module {
    includes(featureLauncherModule)

    includes(featureAccountSettingsModule)
    includes(telemetryModule)
    includes(featureFundingModule)
    includes(onboardingMigrationModule)
    includes(featureMigrationModule)

    single<FundingSettings> { K9FundingSettings() }
}
