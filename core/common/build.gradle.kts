plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(libs.okio)
    implementation(libs.okhttp)
    implementation(libs.okhttp.tls)

    testImplementation(projects.core.testing)
}
