plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.featureflag"
    }
}

codeCoverage {
    lineCoverage = 60
}
