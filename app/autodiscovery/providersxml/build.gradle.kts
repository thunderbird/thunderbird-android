plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.app.core)
    implementation(projects.mail.common)
    implementation(projects.app.autodiscovery.api)

    implementation(libs.timber)

    testImplementation(projects.app.testing)
    testImplementation(projects.backend.imap)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

android {
    namespace = "com.fsck.k9.autodiscovery.providersxml"
}
