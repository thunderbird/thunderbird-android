plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.file"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.uri)

            implementation(projects.core.outcome)

            implementation(libs.kotlinx.io.core)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
        }
    }
}
