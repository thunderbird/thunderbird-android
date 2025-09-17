plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "org.openintents.openpgp"

    buildFeatures {
        aidl = true
    }
}

dependencies {
    api(libs.androidx.lifecycle.common)
    api(libs.androidx.preference)
    api(libs.androidx.fragment)

    implementation(projects.core.logging.implLegacy)
    implementation(libs.androidx.annotation)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
