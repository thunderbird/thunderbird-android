plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding.migration.thunderbird"
    resourcePrefix = "onboarding_migration_thunderbird_"
}

dependencies {
    api(projects.feature.onboarding.migration.api)
    implementation(projects.core.common)
    implementation(projects.core.logging.implLegacy)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.feature.account.common)

    testImplementation(projects.core.ui.compose.testing)
}

codeCoverage {
    branchCoverage.set(34)
}
