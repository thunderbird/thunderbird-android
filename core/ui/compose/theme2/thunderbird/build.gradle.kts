plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "app.k9mail.core.ui.compose.theme2.thunderbird"
        @Suppress("UnstableApiUsage")
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
