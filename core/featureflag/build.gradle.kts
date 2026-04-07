plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.featureflag"
        withHostTest {}
    }
}

codeCoverage {
    lineCoverage = 60
}
