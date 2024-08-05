plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "app.k9mail.legacy.account"
}

dependencies {
    implementation(projects.legacy.notification)
    implementation(projects.mail.common)
    implementation(projects.backend.api)
}
