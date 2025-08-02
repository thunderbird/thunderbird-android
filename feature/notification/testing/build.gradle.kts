plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

android {
    namespace = "net.thunderbird.feature.notification.testing"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.outcome)
            api(projects.feature.notification.api)
        }
    }
}
