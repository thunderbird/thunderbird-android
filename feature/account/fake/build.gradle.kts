plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.account.fake"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
        }
    }
}
