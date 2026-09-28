plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.funding.link"
}

dependencies {
    api(projects.feature.funding.api)

    implementation(projects.core.common)
    implementation(projects.core.android.common)
    implementation(projects.core.logging.api)
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.configstore.api)
    implementation(projects.feature.funding.common)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 5
}
