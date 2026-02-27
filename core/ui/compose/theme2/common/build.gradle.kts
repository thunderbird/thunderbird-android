plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.theme2"
    resourcePrefix = "core_ui_theme2"
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.android.material)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
