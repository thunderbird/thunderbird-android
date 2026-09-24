plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.funding.common"
    resourcePrefix = "funding_"
}

dependencies {
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.ui.theme.api)

    implementation(libs.android.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 5
}
