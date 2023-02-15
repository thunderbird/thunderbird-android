plugins {
    id "thunderbird.library.jvm"
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.lint)
}

dependencies {
    api project(":backend:api")

    api libs.okhttp
    implementation libs.jmap.client
    implementation libs.moshi
    ksp libs.moshi.kotlin.codegen

    testImplementation project(":mail:testing")
    testImplementation project(':backend:testing')
    testImplementation libs.okhttp.mockwebserver
}
