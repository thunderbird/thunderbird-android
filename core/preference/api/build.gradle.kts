plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.preference"
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "DEBUG", "true")
        }

        release {
            buildConfigField("Boolean", "DEBUG", "false")
        }
    }
}
