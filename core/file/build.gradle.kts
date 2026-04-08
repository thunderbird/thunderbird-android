plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
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

codeCoverage {
    branchCoverage = 39
    lineCoverage = 61
}
