plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.funding.noop"
    resourcePrefix = "funding_noop_"
}

dependencies {
    api(projects.feature.funding.api)
}

codeCoverage {
    lineCoverage.set(0)
}
