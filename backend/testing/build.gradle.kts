plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(projects.backend.api)
    implementation(projects.core.common)

    implementation(libs.okio)
    implementation(libs.junit)
    implementation(libs.assertk)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
