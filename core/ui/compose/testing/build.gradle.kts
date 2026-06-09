plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.testing"
}

dependencies {
    api(projects.core.testing)
    api(libs.androidx.compose.ui.test.junit4)

    implementation(projects.core.ui.contract)
    implementation(projects.core.ui.compose.theme2)

    implementation(libs.androidx.test.espresso.core)
    implementation(libs.assertk)
    implementation(libs.jetbrains.compose.ui.test)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.robolectric)
    implementation(libs.turbine)
}

codeCoverage {
    lineCoverage = 0
}
