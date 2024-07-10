plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)

    implementation(libs.preferencex)
    implementation(libs.timber)

    testImplementation(libs.robolectric)
}

android {
    namespace = "app.k9mail.feature.widget.unread"
}
