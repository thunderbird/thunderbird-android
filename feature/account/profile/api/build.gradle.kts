plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.account.profile"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
            api(projects.feature.account.avatar.api)
        }

        androidMain.dependencies {
            // ensure Android target can consume the module if it's platform-specific
            api(projects.feature.account.api)
            api(projects.feature.account.avatar.api)
        }
    }
}
