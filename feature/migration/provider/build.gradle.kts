plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.core)
    implementation(projects.legacy.logging)

    implementation(libs.okio)
}

android {
    namespace = "app.k9mail.feature.migration.provider"
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
