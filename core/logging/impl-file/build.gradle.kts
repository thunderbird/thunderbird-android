plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.logging.file"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.api)
            implementation(projects.core.file)
            implementation(projects.core.outcome)

            implementation(libs.kotlinx.io.core)
            implementation(libs.uri)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
        }
    }
}
