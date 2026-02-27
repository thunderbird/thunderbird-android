plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.ui.compose.theme2"
        @Suppress("UnstableApiUsage")
        androidResources.enable = true
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.android.material)
        }

        commonMain.dependencies {
            implementation(libs.jetbrains.compose.material3)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
