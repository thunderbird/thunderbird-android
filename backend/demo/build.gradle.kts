plugins {
    id "thunderbird.library.jvm"
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.lint)
}

dependencies {
    api project(":backend:api")

    implementation libs.kotlinx.coroutines.core
    implementation libs.moshi
    ksp libs.moshi.kotlin.codegen

    testImplementation project(":mail:testing")
}
