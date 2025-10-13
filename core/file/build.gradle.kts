plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.file"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.outcome)

            implementation(libs.uri)
            implementation(libs.kotlinx.io.core)
        }
        androidUnitTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}
