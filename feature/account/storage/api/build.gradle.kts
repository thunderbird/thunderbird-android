plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.feature.account.storage"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
        }
    }
}
