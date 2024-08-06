plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.ui.theme"
}

dependencies {
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.legacy.preferences)
}
