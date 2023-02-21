plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.designsystem"
    resourcePrefix = "core_ui_designsystem_"
}

dependencies {
    api(projects.core.ui.compose.theme)
    implementation(libs.androidx.compose.material)
}
