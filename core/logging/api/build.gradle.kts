plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.logging"
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.uri)
        }
    }
}
