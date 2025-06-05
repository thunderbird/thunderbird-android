plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.logging.composite"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.api)
        }
    }
}
