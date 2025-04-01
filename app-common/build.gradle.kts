plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.app.common"
}

dependencies {
    api(projects.legacy.common)

    api(projects.feature.account.core)

    implementation(projects.legacy.core)
    implementation(projects.legacy.account)

    implementation(projects.core.featureflags)
    implementation(projects.core.ui.legacy.theme2.common)

    implementation(projects.feature.account.setup)
    implementation(projects.feature.migration.provider)

    implementation(projects.mail.protocols.imap)
}
