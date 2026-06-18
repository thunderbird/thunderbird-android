plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.android.permissions"
}

dependencies {
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}

codeCoverage {
    branchCoverage = 66
    lineCoverage = 71
}
