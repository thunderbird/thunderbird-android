plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.autodiscovery.api)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
