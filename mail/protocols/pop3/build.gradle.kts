plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.mail.common)
    implementation(projects.core.common)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.mail.testing)
    testImplementation(libs.okio)
    testImplementation(libs.jzlib)
    testImplementation(libs.commons.io)
}
