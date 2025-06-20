plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.feature.account.storage.legacy"
}

dependencies {
    api(projects.feature.account.storage.api)

    implementation(projects.feature.notification.api)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)

    implementation(projects.core.logging.api)
    implementation(projects.core.preference.api)

    implementation(projects.mail.common)

    implementation(projects.core.android.account)

    implementation(libs.moshi)

    testImplementation(projects.feature.account.fake)
    testImplementation(projects.mail.protocols.imap)
}
