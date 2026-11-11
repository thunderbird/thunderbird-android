package app.k9mail.feature

import app.k9mail.feature.migration.launcher.featureMigrationLauncherDiModule
import app.k9mail.feature.onboarding.migration.featureOnboardingMigrationDiModule
import app.k9mail.feature.telemetry.featureTelemetryDiModule
import com.fsck.k9.BuildConfig
import net.thunderbird.android.feature.mail.message.reader.api.css.DefaultCssClassNameProvider
import net.thunderbird.app.common.feature.funding.fundingDiModule
import net.thunderbird.feature.account.settings.featureAccountSettingsDiModule
import net.thunderbird.feature.funding.api.FundingSettings
import net.thunderbird.feature.funding.featureFundingDiModule
import net.thunderbird.feature.mail.message.list.internal.featureMailMessageListDiModule
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import org.koin.dsl.module

val featureDiModule = module {
    includes(featureAccountSettingsDiModule)
    includes(featureTelemetryDiModule)
    includes(featureFundingDiModule)
    includes(featureOnboardingMigrationDiModule)
    includes(featureMigrationLauncherDiModule)
    includes(featureMailMessageListDiModule)
    includes(fundingDiModule)

    single<FundingSettings> {
        K9FundingSettings()
    }

    single<CssClassNameProvider> {
        DefaultCssClassNameProvider(
            featureFlagProvider = get(),
            defaultNamespaceClassName = BuildConfig.APPLICATION_ID.replace(oldValue = ".", newValue = "-"),
        )
    }
}
