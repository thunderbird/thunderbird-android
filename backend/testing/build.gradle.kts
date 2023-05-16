plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(projects.backend.api)

    implementation(libs.okio)
    implementation(libs.junit)
    implementation(libs.assertk)
}
