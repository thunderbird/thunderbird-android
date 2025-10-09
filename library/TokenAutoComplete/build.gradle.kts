plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.library.tokenautocomplete"
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
}

codeCoverage {
    branchCoverage.set(6)
    lineCoverage.set(4)
}
