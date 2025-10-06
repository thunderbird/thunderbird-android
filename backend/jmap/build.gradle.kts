plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.backend.api)
    implementation(projects.core.common)
    implementation(projects.feature.mail.folder.api)

    api(libs.okhttp)
    implementation(libs.jmap.client)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.mail.testing)
    testImplementation(projects.backend.testing)
    testImplementation(libs.okhttp.mockwebserver)
}
