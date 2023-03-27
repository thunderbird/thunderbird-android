plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.theme"
    resourcePrefix = "core_ui_theme_"
}

dependencies {
    api(projects.core.ui.compose.common)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.compose.material)
}
