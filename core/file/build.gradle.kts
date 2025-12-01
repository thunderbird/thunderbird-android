plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.file"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.uri)

            implementation(projects.core.outcome)

            implementation(libs.kotlinx.io.core)
        }
        androidUnitTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}
