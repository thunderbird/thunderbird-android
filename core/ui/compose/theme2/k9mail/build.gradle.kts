plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.ui.compose.theme2.k9mail"
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.ui.compose.theme2.common)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "net.thunderbird.core.ui.compose.theme2.k9mail.resources"
    generateResClass = always
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
