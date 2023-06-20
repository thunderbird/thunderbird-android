plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.mail.common)
    implementation(projects.feature.autodiscovery.api)

    implementation(libs.timber)

    testImplementation(projects.core.android.testing)
    testImplementation(projects.backend.imap)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

android {
    namespace = "app.k9mail.autodiscovery.providersxml"
}
