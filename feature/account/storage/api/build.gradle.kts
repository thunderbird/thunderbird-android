plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.account.storage"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.profile.api)
        }
    }
}
