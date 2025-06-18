plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.outcome)
            implementation(projects.feature.notification.api)
        }
    }
}

android {
    namespace = "net.thunderbird.feature.notification"
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources"
}
