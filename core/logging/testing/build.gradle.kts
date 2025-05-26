plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.logging.testing"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.logging.api)
        }
    }
}
