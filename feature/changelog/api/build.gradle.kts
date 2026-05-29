plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.changelog.api"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui.navigation)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
