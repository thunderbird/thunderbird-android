plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.autodiscovery.api)

    compileOnly(libs.xmlpull)
    implementation(libs.okhttp)
    implementation(libs.minidns.hla)

    testImplementation(libs.kxml2)
    testImplementation(libs.jsoup)
    testImplementation(libs.okhttp.mockwebserver)
}
