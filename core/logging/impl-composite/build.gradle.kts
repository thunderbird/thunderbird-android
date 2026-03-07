plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.logging.composite"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.api)
        }
    }
}
