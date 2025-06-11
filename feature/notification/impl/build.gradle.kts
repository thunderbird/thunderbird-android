plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.outcome)
            implementation(projects.core.logging.api)
            implementation(projects.feature.account.api)
            implementation(projects.feature.notification.api)
        }
    }
}

android {
    namespace = "net.thunderbird.feature.notification.impl"
}
