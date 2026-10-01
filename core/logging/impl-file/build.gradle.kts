plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.logging.file"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.api)
            implementation(projects.core.file)

            implementation(libs.kotlinx.io.core)
            implementation(libs.uri)
        }
        androidHostTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}

codeCoverage {
    branchCoverage = 50
    lineCoverage = 71
}
