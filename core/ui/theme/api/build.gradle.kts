plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.ui.theme.api"
    }
}

codeCoverage {
    lineCoverage.set(0)
}
