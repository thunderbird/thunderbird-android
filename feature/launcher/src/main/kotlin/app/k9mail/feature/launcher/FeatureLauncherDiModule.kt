package app.k9mail.feature.launcher

import app.k9mail.feature.account.edit.featureAccountEditDiModule
import app.k9mail.feature.account.setup.featureAccountSetupDiModule
import app.k9mail.feature.onboarding.main.featureOnboardingMainDiModule
import app.k9mail.feature.settings.import.featureSettingsImportDiModule
import net.thunderbird.feature.debug.settings.featureDebugSettingsDiModule
import org.koin.dsl.module

val featureLauncherDiModule = module {
    includes(
        featureOnboardingMainDiModule,
        featureSettingsImportDiModule,
        featureAccountSetupDiModule,
        featureAccountEditDiModule,
        featureDebugSettingsDiModule,
    )
}
