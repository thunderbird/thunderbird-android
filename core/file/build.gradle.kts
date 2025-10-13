plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.file"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
        }
    }
}
