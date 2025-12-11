plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.android.permissions"
}

dependencies {
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.assertk)
}

codeCoverage {
    branchCoverage.set(66)
    lineCoverage.set(71)
}
