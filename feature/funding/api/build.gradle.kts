plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.funding.api"
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.ui.navigation)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
