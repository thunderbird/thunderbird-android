plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding.main"
    resourcePrefix = "onboarding_main_"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.compose.navigation)
    implementation(projects.feature.onboarding.welcome)
    implementation(projects.feature.account.setup)
    implementation(projects.feature.settings.import)
    implementation(projects.feature.onboarding.permissions)
    implementation(projects.feature.onboarding.migration.api)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
