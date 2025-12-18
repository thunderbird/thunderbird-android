plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.core)

    implementation(libs.okio)
}

android {
    namespace = "app.k9mail.feature.migration.provider"
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
