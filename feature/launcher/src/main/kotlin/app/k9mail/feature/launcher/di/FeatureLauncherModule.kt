package app.k9mail.feature.launcher.di

import app.k9mail.feature.account.edit.featureAccountEditModule
import app.k9mail.feature.account.setup.featureAccountSetupModule
import app.k9mail.feature.onboarding.main.featureOnboardingModule
import app.k9mail.feature.settings.import.featureSettingsImportModule
import net.thunderbird.feature.debug.settings.inject.featureDebugSettingsModule
import org.koin.dsl.module

val featureLauncherModule = module {
    includes(
        featureOnboardingModule,
        featureSettingsImportModule,
        featureAccountSetupModule,
        featureAccountEditModule,
        featureDebugSettingsModule,
    )
}
