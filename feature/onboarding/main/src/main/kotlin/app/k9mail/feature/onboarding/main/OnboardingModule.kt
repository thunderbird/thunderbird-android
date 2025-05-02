package app.k9mail.feature.onboarding.main

import app.k9mail.feature.onboarding.main.navigation.DefaultOnboardingNavigation
import app.k9mail.feature.onboarding.main.navigation.OnboardingNavigation
import app.k9mail.feature.onboarding.permissions.featureOnboardingPermissionsModule
import org.koin.core.module.Module
import org.koin.dsl.module

val featureOnboardingModule: Module = module {
    single<OnboardingNavigation> { DefaultOnboardingNavigation() }

    includes(
        featureOnboardingPermissionsModule,
    )
}
