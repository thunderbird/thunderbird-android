plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.preference.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.preference.api)

            implementation(projects.core.logging.api)
        }
    }
}
