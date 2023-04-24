plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.app.autodiscovery.api)

    implementation(libs.minidns.hla)
}
