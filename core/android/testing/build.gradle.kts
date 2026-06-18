plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.testing"
}

dependencies {
    implementation(projects.core.logging.api)
    implementation(projects.core.preference.api)
    implementation(projects.core.preference.impl)

    implementation(libs.kotlin.test.junit)
    implementation(libs.robolectric)

    implementation(libs.mockito.kotlin)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
