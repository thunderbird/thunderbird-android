plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.thunderbird.feature.changelog.internal"
}

dependencies {
    implementation(projects.core.ui.contract)
    implementation(projects.core.ui.navigation)
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.preference.api)
    implementation(projects.feature.changelog.api)

    implementation(libs.ckchangelog.core)
}
codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
