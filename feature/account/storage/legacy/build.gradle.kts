plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.feature.account.storage.legacy"
}

dependencies {
    implementation(projects.core.preferences)

    implementation(projects.mail.common)

    implementation(projects.core.android.account)

    implementation(libs.moshi)

    testImplementation(projects.mail.protocols.imap)
}
