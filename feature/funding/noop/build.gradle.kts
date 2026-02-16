plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.funding.noop"
    resourcePrefix = "funding_noop_"
}

dependencies {
    api(projects.feature.funding.api)
}

codeCoverage {
    lineCoverage = 0
}
