plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.ui.setting"
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
