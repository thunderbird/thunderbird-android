plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.autodiscovery.autoconfig)

    testImplementation(libs.kxml2)
}

codeCoverage {
    branchCoverage = 64
    lineCoverage = 60
}
