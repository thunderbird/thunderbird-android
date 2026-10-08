plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.ui.theme.api"
    }
}

codeCoverage {
    lineCoverage = 0
}
