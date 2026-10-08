plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.ui.setting"
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
