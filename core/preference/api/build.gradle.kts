plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.preference"
    buildFeatures {
        buildConfig = true
    }
}
