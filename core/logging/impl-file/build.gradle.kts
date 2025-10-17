plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.logging.file"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.api)
            implementation(projects.core.file)
            implementation(projects.core.outcome)

            implementation(libs.kotlinx.io.core)
            implementation(libs.uri)
        }
        androidUnitTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}
