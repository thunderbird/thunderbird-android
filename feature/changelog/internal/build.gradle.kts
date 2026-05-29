plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.thunderbird.feature.changelog.internal"
}

dependencies {
    implementation(projects.core.ui.navigation)
    implementation(libs.ckchangelog.core)
    api(projects.core.ui.contract)
    implementation(projects.feature.changelog.api)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.preference.api)
}
codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
