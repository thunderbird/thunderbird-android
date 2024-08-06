plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.ui.folder"
}

dependencies {
    implementation(projects.core.ui.legacy.designsystem)
    implementation(projects.legacy.folder)
}
