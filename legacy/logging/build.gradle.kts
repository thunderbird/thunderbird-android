plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.logging.legacy"
    }
}

codeCoverage {
    branchCoverage = 30
}
