plugins {
    id "thunderbird.library.android"
    alias(libs.plugins.kotlin.parcelize)
}

dependencies {
    api project(":mail:common")
    api project(":backend:api")
    api project(":app:html-cleaner")

    implementation project(':plugins:openpgp-api-lib:openpgp-api')

    api libs.koin.android

    api libs.androidx.annotation

    implementation libs.okio
    implementation libs.commons.io
    implementation libs.androidx.core.ktx
    implementation libs.androidx.work.ktx
    implementation libs.androidx.fragment
    implementation libs.androidx.localbroadcastmanager
    implementation libs.jsoup
    implementation libs.moshi
    implementation libs.timber
    implementation libs.mime4j.core
    implementation libs.mime4j.dom

    testImplementation project(':mail:testing')
    testImplementation project(":backend:imap")
    testImplementation project(":mail:protocols:smtp")
    testImplementation project(":app:storage")
    testImplementation project(":app:testing")
    testImplementation libs.kotlin.test
    testImplementation libs.kotlin.reflect
    testImplementation libs.robolectric
    testImplementation libs.androidx.test.core
    testImplementation libs.jdom2
}

android {
    namespace 'com.fsck.k9.core'

    buildFeatures {
        buildConfig true
    }
}
