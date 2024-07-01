plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(projects.app.core)
    api(projects.app.ui.base)
    api(projects.app.ui.legacy)
    api(projects.mail.common)

    api(libs.preferencex)

    implementation(projects.core.ui.legacy.designsystem)

    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.preference)
    implementation(libs.timber)

    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows)
}

android {
    namespace = "app.k9mail.feature.widget.unread"
}
