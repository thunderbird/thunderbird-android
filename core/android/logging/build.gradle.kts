plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.logging"
}

dependencies {
    implementation(libs.timber)
    implementation(libs.commons.io)
}

codeCoverage {
    branchCoverage.set(50)
    lineCoverage.set(48)
}
