plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.account.avatar"
    }

    sourceSets {

        commonMain.dependencies {
            api(projects.feature.account.api)
            api(libs.uri)
        }
    }
}
