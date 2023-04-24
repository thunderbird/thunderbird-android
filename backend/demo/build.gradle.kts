plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.backend.api)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)

    testImplementation(projects.mail.testing)
}
