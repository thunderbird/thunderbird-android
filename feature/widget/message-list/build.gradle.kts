plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(projects.app.core)

    implementation(projects.app.ui.legacy)
    implementation(projects.core.ui.legacy.designsystem)
    implementation(projects.mail.common)

    implementation(libs.androidx.appcompat)
    implementation(libs.timber)
}

android {
    namespace = "app.k9mail.feature.widget.message.list"
}
