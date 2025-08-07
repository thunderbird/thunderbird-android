plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.logging.legacy"
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.timber)
        }

        commonMain.dependencies {
            api(projects.core.logging.api)
            api(libs.androidx.annotation)
        }

        commonTest.dependencies {
            implementation(projects.core.logging.testing)
        }
    }
}
