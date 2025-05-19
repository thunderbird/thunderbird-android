plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.account.api"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.architecture.api)
        }
    }
}
