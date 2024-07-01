plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(projects.app.ui.legacy)

    implementation(projects.app.core)
    implementation(projects.core.ui.legacy.theme2.common)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.viewmodel)
}

android {
    namespace = "app.k9mail.feature.widget.shortcut"
}
