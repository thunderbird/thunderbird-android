plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.ui.account"
}

dependencies {
    implementation(projects.core.ui.legacy.designsystem)

    implementation(libs.glide)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
