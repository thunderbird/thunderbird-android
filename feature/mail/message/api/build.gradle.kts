plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.mail.message"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.architecture.api)
        }
    }
}
