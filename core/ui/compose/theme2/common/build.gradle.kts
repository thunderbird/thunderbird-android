plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.theme2"
    resourcePrefix = "core_ui_theme2"
}

dependencies {
    api(projects.core.ui.compose.common)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.android.material)

    implementation(libs.androidx.activity)
}
