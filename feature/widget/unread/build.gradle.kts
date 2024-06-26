plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.app.ui.legacy)
    implementation(projects.app.core)

    implementation(libs.preferencex)
    implementation(libs.timber)

    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows)
}

android {
    namespace = "app.k9mail.feature.widget.unread"
}
