plugins {
    id("thunderbird.library.kmp.compose")
}

group = "net.thunderbird.components.ui"
version = libs.versions.tbMobileComponents.get()

kotlin {
    explicitApi()

    android {
        namespace = "net.thunderbird.components.ui.testing"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.test)
            api(libs.jetbrains.compose.components.resources)

            implementation(libs.jetbrains.compose.ui.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlin.test.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.androidx.test.espresso.core)
        }

        jvmMain.dependencies {
            implementation(libs.androidx.navigationevent)
        }
    }
}

codeCoverage {
    lineCoverage = 65
}
