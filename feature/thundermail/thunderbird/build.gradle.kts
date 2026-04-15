plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.dev.mokkery)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.thundermail.thunderbird"
        androidResources.enable = true
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.thundermail.api)
            implementation(projects.core.ui.compose.theme2.common)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.thundermail.thunderbird.resources"
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
