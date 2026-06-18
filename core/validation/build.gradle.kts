plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.validation"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.outcome)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.reflect)
        }
    }
}

codeCoverage {
    branchCoverage = 68
}
