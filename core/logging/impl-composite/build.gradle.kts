plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.logging.composite"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.api)
        }
    }
}
