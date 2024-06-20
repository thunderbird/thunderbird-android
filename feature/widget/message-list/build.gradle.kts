plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.app.ui.legacy)
    implementation(projects.app.core)

    implementation(libs.timber)
}

android {
    namespace = "app.k9mail.feature.widget.message.list"
}
