plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.feature.account"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.architecture.api)
        }
    }
}
