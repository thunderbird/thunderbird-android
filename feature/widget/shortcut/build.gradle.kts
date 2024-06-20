plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.app.ui.legacy)
    implementation(projects.app.core)
}

android {
    namespace = "app.k9mail.feature.widget.shortcut"
}
