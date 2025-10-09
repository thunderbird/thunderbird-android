plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.funding.link"
    resourcePrefix = "funding_link_"
}

dependencies {
    api(projects.feature.funding.api)
}

codeCoverage {
    lineCoverage.set(0)
}
