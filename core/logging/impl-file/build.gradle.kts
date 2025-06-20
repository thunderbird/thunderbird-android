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
        }
        commonTest.dependencies {
            implementation(libs.bundles.shared.jvm.test)
        }
    }
}
