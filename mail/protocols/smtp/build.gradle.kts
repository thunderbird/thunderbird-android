plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.mail.common)
    implementation(projects.core.common)
    implementation(projects.legacy.logging)

    implementation(libs.commons.io)
    implementation(libs.okio)

    testImplementation(projects.mail.testing)

    testImplementation(libs.jzlib)
    testImplementation(libs.mockito.kotlin)
}
