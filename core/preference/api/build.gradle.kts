plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
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
    branchCoverage.set(0)
    lineCoverage.set(0)
}
