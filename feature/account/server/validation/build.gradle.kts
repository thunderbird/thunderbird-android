plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.server.validation"
    resourcePrefix = "account_server_validation_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.common)

    implementation(projects.mail.common)
    implementation(projects.mail.protocols.imap)
    implementation(projects.mail.protocols.pop3)
    implementation(projects.mail.protocols.smtp)

    implementation(projects.feature.account.common)
    implementation(projects.feature.account.oauth)
    implementation(projects.feature.account.server.certificate)

    testImplementation(projects.core.ui.compose.testing)
}
