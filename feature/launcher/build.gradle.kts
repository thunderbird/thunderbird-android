plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.launcher"
    resourcePrefix = "launcher_"
}

dependencies {
    api(projects.feature.changelog.api)
    implementation(projects.legacy.ui.base)
    implementation(projects.feature.onboarding.main)
    implementation(projects.feature.thundermail.api)
    implementation(projects.feature.settings.import)

    implementation(projects.feature.account.edit)
    implementation(projects.feature.account.settings.api)
    implementation(projects.feature.account.setup)

    implementation(projects.feature.funding.api)
    implementation(projects.feature.debugSettings)

    implementation(libs.androidx.activity.compose)

    testImplementation(projects.core.ui.compose.testing)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
