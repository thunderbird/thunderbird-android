plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.network"
}

dependencies {
    api(projects.core.common)

    implementation(projects.core.logging.api)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
