plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.configstore.testing"
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
