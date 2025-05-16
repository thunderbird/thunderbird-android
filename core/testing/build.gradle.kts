plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.testing"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.assertk)
            implementation(libs.turbine)
        }
    }
}
