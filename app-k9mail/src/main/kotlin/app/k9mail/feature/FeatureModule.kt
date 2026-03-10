package app.k9mail.feature

import app.k9mail.feature.funding.api.FundingSettings
import app.k9mail.feature.funding.featureFundingModule
import app.k9mail.feature.migration.launcher.featureMigrationModule
import app.k9mail.feature.onboarding.migration.onboardingMigrationModule
import app.k9mail.feature.telemetry.telemetryModule
import com.fsck.k9.BuildConfig
import net.thunderbird.android.feature.mail.message.reader.api.css.DefaultCssClassNameProvider
import net.thunderbird.feature.account.settings.featureAccountSettingsModule
import net.thunderbird.feature.mail.message.list.internal.featureMessageListModule
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import org.koin.dsl.module

val featureModule = module {
    includes(featureAccountSettingsModule)
    includes(telemetryModule)
    includes(featureFundingModule)
    includes(onboardingMigrationModule)
    includes(featureMigrationModule)
    includes(featureMessageListModule)

    single<FundingSettings> { K9FundingSettings() }

    single<CssClassNameProvider> {
        DefaultCssClassNameProvider(
            featureFlagProvider = get(),
            defaultNamespaceClassName = BuildConfig.APPLICATION_ID.replace(".", "-"),
        )
    }
}
