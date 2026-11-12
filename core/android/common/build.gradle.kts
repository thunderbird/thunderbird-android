plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.common"
}

dependencies {
    api(projects.core.common)

    implementation(libs.androidx.webkit)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.kotlin)
}

codeCoverage {
    branchCoverage = 51
    lineCoverage = 55
}
