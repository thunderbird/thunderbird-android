plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.funding.link"
    resourcePrefix = "funding_link_"
}

dependencies {
    api(projects.feature.funding.api)
}

codeCoverage {
    lineCoverage = 0
}
