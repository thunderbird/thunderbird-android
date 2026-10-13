plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.backend.api)
    implementation(projects.core.common)
    implementation(projects.core.logging.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.mail.common)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    testImplementation(projects.backend.testing)
    testImplementation(projects.core.logging.testing)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
