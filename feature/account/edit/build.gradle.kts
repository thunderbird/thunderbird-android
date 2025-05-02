plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.edit"
    resourcePrefix = "account_edit_"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.compose.navigation)

    implementation(projects.mail.common)

    implementation(projects.feature.account.common)
    implementation(projects.feature.account.oauth)
    implementation(projects.feature.account.server.settings)
    implementation(projects.feature.account.server.certificate)
    implementation(projects.feature.account.server.validation)

    testImplementation(projects.core.ui.compose.testing)
    testImplementation(projects.mail.protocols.imap)
}
