plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

codeCoverage {
    lineCoverage.set(0)
}
