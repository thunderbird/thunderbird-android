plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.preference"
    buildFeatures {
        buildConfig = true
    }
}
