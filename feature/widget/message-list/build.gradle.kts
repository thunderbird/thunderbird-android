plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)

    implementation(libs.timber)
}

android {
    namespace = "app.k9mail.feature.widget.message.list"
}
