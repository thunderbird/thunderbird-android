plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.preference"
    buildFeatures {
        buildConfig = true
    }
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
