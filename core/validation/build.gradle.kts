plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.validation"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.outcome)
        }
    }
}
