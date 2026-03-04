plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.preference"
        withHostTest {}
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
