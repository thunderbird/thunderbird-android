plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.autodiscovery.api)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp)
    implementation(libs.minidns.hla)
    compileOnly(libs.xmlpull)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kxml2)
    testImplementation(libs.jsoup)
    testImplementation(libs.okhttp.mockwebserver)
}
