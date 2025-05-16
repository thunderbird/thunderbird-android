plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.thunderbird.core.android.account"
}

dependencies {
    api(projects.feature.notification)
    api(projects.mail.common)

    implementation(projects.core.preferences)
    implementation(projects.core.mail.folder.api)

    implementation(projects.feature.mail.account.api)

    implementation(projects.backend.api)
}
