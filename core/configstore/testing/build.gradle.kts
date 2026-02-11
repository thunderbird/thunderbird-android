plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.configstore.testing"
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.configstore.api)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
