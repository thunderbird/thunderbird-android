plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

android {
    namespace = "net.thunderbird.core.ui.theme.api"
}

codeCoverage {
    lineCoverage.set(0)
}
