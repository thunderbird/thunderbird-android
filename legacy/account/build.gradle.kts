plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "app.k9mail.legacy.account"
}

dependencies {
    api(projects.legacy.notification)
    api(projects.mail.common)

    implementation(projects.backend.api)
}
