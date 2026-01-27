plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.testing"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.assertk)
            implementation(libs.turbine)
        }
    }
}
