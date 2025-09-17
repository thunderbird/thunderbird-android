plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(libs.androidx.appcompat)
    api(libs.android.material)
    api(libs.androidx.coordinatorlayout)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel)
}

android {
    namespace = "app.k9mail.ui.utils.bottomsheet"
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
