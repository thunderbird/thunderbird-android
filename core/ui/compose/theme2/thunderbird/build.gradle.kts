plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.ui.compose.theme2.thunderbird"
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.ui.compose.theme2.common)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
