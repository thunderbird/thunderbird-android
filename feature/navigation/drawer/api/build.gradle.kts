plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.navigation.drawer.api"
    resourcePrefix = "navigation_drawer_"
}

codeCoverage {
    lineCoverage.set(0)
}
