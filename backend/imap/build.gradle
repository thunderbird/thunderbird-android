plugins {
    id "thunderbird.library.jvm"
    alias(libs.plugins.android.lint)
}

dependencies {
    api project(":backend:api")
    api project(":mail:protocols:imap")
    api project(":mail:protocols:smtp")

    implementation libs.kotlinx.coroutines.core

    testImplementation project(":mail:testing")
    testImplementation project(":backend:testing")
    testImplementation libs.mime4j.dom
}
