plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    androidLibrary {
        namespace = "app.k9mail.core.ui.compose.navigation"
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
