package app.k9mail.feature.launcher.di

import app.k9mail.feature.account.edit.featureAccountEditModule
import app.k9mail.feature.account.setup.featureAccountSetupModule
import app.k9mail.feature.onboarding.main.featureOnboardingModule
import org.koin.dsl.module

val featureLauncherModule = module {
    includes(
        featureOnboardingModule,
        featureAccountSetupModule,
        featureAccountEditModule,
    )
}
