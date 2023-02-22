plugins {
    id "thunderbird.library.jvm"
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation project(":backend:api")

    implementation libs.okio
    implementation libs.junit
}
