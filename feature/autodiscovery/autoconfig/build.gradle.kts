plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.autodiscovery.api)
    api(libs.okhttp)

    implementation(libs.minidns.hla)
    compileOnly(libs.xmlpull)

    testImplementation(projects.core.logging.testing)
    testImplementation(libs.kxml2)
    testImplementation(libs.jsoup)
    testImplementation(libs.okhttp.mockwebserver)
}
