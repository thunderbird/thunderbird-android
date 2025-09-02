plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.backend.api)
    implementation(projects.feature.mail.folder.api)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.mail.testing)
}
