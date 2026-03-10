plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.autodiscovery.api)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
