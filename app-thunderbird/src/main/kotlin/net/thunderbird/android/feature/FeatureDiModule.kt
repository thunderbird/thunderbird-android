package net.thunderbird.android.feature

import app.k9mail.feature.migration.launcher.featureMigrationLauncherDiModule
import app.k9mail.feature.onboarding.migration.featureOnboardingMigrationDiModule
import app.k9mail.feature.telemetry.featureTelemetryDiModule
import net.thunderbird.android.BuildConfig
import net.thunderbird.app.common.feature.mail.message.reader.DefaultCssClassNameProvider
import net.thunderbird.feature.account.settings.featureAccountSettingsDiModule
import net.thunderbird.feature.funding.api.FundingSettings
import net.thunderbird.feature.funding.featureFundingDiModule
import net.thunderbird.feature.mail.message.list.internal.featureMailMessageListDiModule
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import org.koin.dsl.module

internal val featureDiModule = module {
    includes(featureAccountSettingsDiModule)
    includes(featureTelemetryDiModule)
    includes(featureFundingDiModule)
    includes(featureOnboardingMigrationDiModule)
    includes(featureMigrationLauncherDiModule)
    includes(featureMailMessageListDiModule)

    single<FundingSettings> {
        TbFundingSettings(
            fundingConfigStore = get(),
        )
    }

    single<CssClassNameProvider> {
        DefaultCssClassNameProvider(
            featureFlagProvider = get(),
            defaultNamespaceClassName = BuildConfig.APPLICATION_ID.replace(oldValue = ".", newValue = "-"),
        )
    }
}
