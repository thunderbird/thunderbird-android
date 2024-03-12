plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.theme2.thunderbird"
    resourcePrefix = "core_ui_theme2_thunderbird"
}

dependencies {
    api(projects.core.ui.compose.theme2.common)
}
