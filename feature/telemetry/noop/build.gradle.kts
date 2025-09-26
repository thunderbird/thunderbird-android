plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.telemetry.api)
}

codeCoverage {
    lineCoverage.set(0)
}
