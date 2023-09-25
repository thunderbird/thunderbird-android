plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.theme.material2"
    resourcePrefix = "core_ui_theme_material2_"
}

dependencies {
    api(projects.core.ui.compose.common)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)
}
