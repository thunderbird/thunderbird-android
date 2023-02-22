plugins {
    id "thunderbird.library.jvm"
    alias(libs.plugins.android.lint)
}

dependencies {
    api project(":backend:api")
    api project(":mail:protocols:webdav")

    testImplementation project(":mail:testing")
}
