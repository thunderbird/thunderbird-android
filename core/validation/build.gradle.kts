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
    }
}

codeCoverage {
    branchCoverage = 68
}
