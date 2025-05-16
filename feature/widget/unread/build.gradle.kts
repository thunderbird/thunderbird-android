plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.feature.mail.account.api)

    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)
    implementation(projects.core.android.account)

    implementation(libs.preferencex)
    implementation(libs.timber)

    testImplementation(libs.robolectric)
}

android {
    namespace = "app.k9mail.feature.widget.unread"
}
