plugins {
    id "thunderbird.library.jvm"
    alias(libs.plugins.android.lint)
}

if (rootProject.testCoverage) {
    apply plugin: 'jacoco'
}

dependencies {
    api libs.jetbrains.annotations

    implementation libs.mime4j.core
    implementation libs.mime4j.dom
    implementation libs.okio
    implementation libs.commons.io
    implementation libs.moshi

    // We're only using this for its DefaultHostnameVerifier
    implementation libs.apache.httpclient5

    testImplementation project(":mail:testing")
    testImplementation libs.icu4j.charset
}
