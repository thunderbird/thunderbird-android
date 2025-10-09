plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.account.api)
}

codeCoverage {
    lineCoverage.set(0)
}
