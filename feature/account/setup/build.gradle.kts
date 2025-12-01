plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.setup"
    resourcePrefix = "account_setup_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.validation)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.compose.navigation)

    implementation(projects.mail.common)
    implementation(projects.mail.protocols.imap)
    implementation(projects.mail.protocols.pop3)
    implementation(projects.mail.protocols.smtp)

    implementation(projects.feature.autodiscovery.service)
    implementation(projects.feature.autodiscovery.demo)

    api(projects.feature.account.common)
    implementation(projects.feature.account.oauth)
    implementation(projects.feature.account.server.settings)
    implementation(projects.feature.account.server.certificate)
    api(projects.feature.account.server.validation)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.core.ui.compose.testing)

    testImplementation(platform(libs.forkhandles.bom))
    testImplementation(libs.forkhandles.fabrikate4k)
}
