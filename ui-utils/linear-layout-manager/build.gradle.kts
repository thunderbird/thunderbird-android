plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(libs.androidx.recyclerview)

    implementation(libs.androidx.annotation)
}

android {
    namespace = "app.k9mail.ui.utils.linearlayoutmanager"
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
