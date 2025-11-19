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
            api(projects.core.outcome)

            implementation(libs.kotlinx.io.core)
        }
        androidHostTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}
