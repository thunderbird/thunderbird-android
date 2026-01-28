plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.outcome"
        withHostTest {}
    }
}

codeCoverage {
    branchCoverage = 28
    lineCoverage = 53
}
