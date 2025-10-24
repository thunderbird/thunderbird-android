plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.outcome)
        }
    }
}

android {
    namespace = "net.thunderbird.core.validation"
}
