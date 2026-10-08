plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.testing"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
            implementation(libs.turbine)
        }
    }
}

codeCoverage {
    lineCoverage = 68
}
