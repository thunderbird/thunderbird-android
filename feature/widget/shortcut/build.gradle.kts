plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)
    implementation(projects.core.android.account)
}

android {
    namespace = "app.k9mail.feature.widget.shortcut"
}
