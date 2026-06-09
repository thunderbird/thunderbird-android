plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.outcome"
    }
}

codeCoverage {
    branchCoverage = 28
    lineCoverage = 53
}
