plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.logging.console"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.api)
        }

        androidMain.dependencies {
            implementation(libs.timber)
        }

        androidUnitTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}
