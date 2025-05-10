plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.feature.account.storage.legacy"
}

dependencies {
    api(projects.feature.account.storage.api)

    implementation(projects.core.preferences)

    implementation(projects.mail.common)

    implementation(projects.core.android.account)

    implementation(libs.moshi)

    testImplementation(projects.mail.protocols.imap)
}
