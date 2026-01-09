plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.account"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.architecture.api)
        }

        androidMain.dependencies {
            // ensure Android target can consume the module if it's platform-specific
            api(projects.core.architecture.api)
        }
    }
}
