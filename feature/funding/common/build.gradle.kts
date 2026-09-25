plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.funding.common"
    resourcePrefix = "funding_"
}

dependencies {
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.ui.theme.api)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 5
}
