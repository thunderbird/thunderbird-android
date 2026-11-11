package app.k9mail.feature.onboarding.main

import app.k9mail.feature.onboarding.main.navigation.DefaultOnboardingNavigation
import app.k9mail.feature.onboarding.main.navigation.OnboardingNavigation
import app.k9mail.feature.onboarding.permissions.featureOnboardingPermissionsDiModule
import org.koin.core.module.Module
import org.koin.dsl.module

val featureOnboardingMainDiModule: Module = module {
    single<OnboardingNavigation> { DefaultOnboardingNavigation() }

    includes(
        featureOnboardingPermissionsDiModule,
    )
}
