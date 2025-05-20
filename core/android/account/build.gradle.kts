plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.thunderbird.core.android.account"
}

dependencies {
    api(projects.feature.account.api)
    api(projects.feature.account.storage.api)

    api(projects.feature.notification)
    api(projects.mail.common)

    implementation(projects.core.preferences)

    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)

    implementation(projects.backend.api)

    testImplementation(projects.feature.account.fake)
}
