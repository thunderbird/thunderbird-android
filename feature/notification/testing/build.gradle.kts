plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

android {
    namespace = "net.thunderbird.feature.notification.testing"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.notification.api)
        }
    }
}
