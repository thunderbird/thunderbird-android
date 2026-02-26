plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "net.thunderbird.core.ui.compose.navigation"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.compose.navigation)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
