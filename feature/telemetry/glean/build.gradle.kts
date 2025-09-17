plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.feature.telemetry.glean"
    resourcePrefix = "telemetry_glean_"
}

dependencies {
    api(projects.feature.telemetry.api)
    api(libs.okhttp)

    implementation(libs.mozilla.components.glean)
    implementation(libs.mozilla.components.fetch.okhttp)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
