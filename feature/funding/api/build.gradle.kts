plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.thunderbird.feature.funding.api"
    resourcePrefix = "funding_api_"
}

dependencies {
    api(projects.core.ui.compose.navigation)
}

codeCoverage {
    lineCoverage = 0
}
