plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.feature.account.core"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
        }
    }
}
