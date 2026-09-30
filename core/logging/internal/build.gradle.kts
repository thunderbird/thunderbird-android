plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.logging.internal"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.api)
            implementation(projects.core.common)
            implementation(libs.tb.mobile.components.core.logging.file)
            implementation(libs.filekit.core)
            implementation(libs.uri)
        }
        androidMain.dependencies {
            implementation(libs.commons.io)
        }
        androidHostTest.dependencies {
            implementation(libs.mockito.kotlin)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
