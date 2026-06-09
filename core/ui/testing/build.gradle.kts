plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    explicitApi()

    android {
        namespace = "net.thunderbird.core.ui.testing"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.test)

            implementation(projects.core.testing)
            implementation(libs.jetbrains.compose.ui.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlin.test.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.compose.ui.test.manifest)
        }
    }
}

codeCoverage {
    lineCoverage = 65
}
