plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.app.autodiscovery.api)

    compileOnly(libs.xmlpull)
    implementation(libs.okhttp)

    testImplementation(libs.kxml2)
    testImplementation(libs.okhttp.mockwebserver)
}
